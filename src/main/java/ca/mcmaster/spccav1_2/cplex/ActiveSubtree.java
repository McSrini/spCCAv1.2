/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_2.cplex;
   
import static ca.mcmaster.spccav1_2.Driver.*;
import ca.mcmaster.spccav1_2.UtilityLibrary;
import ca.mcmaster.spccav1_2.cca.IndexNode;
import ca.mcmaster.spccav1_2.cca.IndexTree;
import ca.mcmaster.spccav1_2.cplex.callbacks.*;
import ca.mcmaster.spccav1_2.cplex.datatypes.*;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.List;
import java.util.Map; 

/**
 *
 * @author tamvadss
 */
public class ActiveSubtree {
    
    private IloCplex cplex   ;
    
    //this is the branch handler for the CPLEX object
    private BranchHandler branchHandler;
    private NodeHandler nodeHandler;
    
    public IndexTree indexTree=null;
    
    public ActiveSubtree (IloCplex cplex  , Map< String, Double >   lowerBounds, Map< String, Double >   upperBounds) throws Exception{
            
        this.cplex=cplex;
        if (upperBounds!=null && lowerBounds!=null) UtilityLibrary.merge(cplex,  lowerBounds, upperBounds); 
        
        branchHandler = new BranchHandler(    );
        nodeHandler = new NodeHandler(    );       
         
        this.cplex.use(branchHandler);
        this.cplex.use(nodeHandler);         
    
    }
       
    //note that this method can be called again and again, if you want you can change NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE
    public List<IndexNode> getCCANodes () {
        indexTree.splitToCCA();
        return indexTree.getCCANodes();
    }
  
    public void solve() throws IloException{
        //cplex.setParam( IloCplex.Param.MIP.Strategy.Backtrack,  ZERO); 
        cplex.solve();
        this.indexTree = nodeHandler.indexTree;
    }
    
    public int getNumBranches () {
        return this.branchHandler.numBranches;
    }
    
    public double getBestObjValue(){
        return this.branchHandler.bestReaminingObjValue;
    }
    
    public long getNumNodesSolved() {
        return this.nodeHandler.numNodesSolved;
    }
    
    //prune all descendants of the selected CCA node
    public void selectCCANode (IndexNode  selectedCCANode) {
        List<NodeAttachment> alleafs = new ArrayList<NodeAttachment> ( ) ;
        alleafs.addAll(  selectedCCANode.leafNodesToTheLeft );
        alleafs.addAll(  selectedCCANode.leafNodesToTheRight );
        for (NodeAttachment node : alleafs) {
            branchHandler.pruneList.add(node.nodeID );
        }
    }
    
    public double getSolution () throws IloException {
        double cutoff = IS_MAXIMIZATION? -BILLION : BILLION;
        boolean isFeasible= this.cplex.getStatus().equals(IloCplex.Status.Feasible);
        boolean isOptimal= this.cplex.getStatus().equals(IloCplex.Status.Optimal);
        if (isFeasible||isOptimal) cutoff=this.cplex.getObjValue();
        return cutoff;
    }
    
    public void setUpperCutoff (double cutoff) throws IloException {
        //cplex.setParam(    IloCplex.Param.MIP.Tolerances.UpperCutoff, cutoff);
        this.branchHandler.cutoff=cutoff;
    }
    
    public void solveFor2Min() throws IloException{
       //cplex.setParam( IloCplex.Param.MIP.Strategy.Backtrack,  ZERO); 
        cplex.setParam(IloCplex.Param.TimeLimit, TWO*SIXTY); 
        cplex.solve();
     
    }
        
    public boolean isOptimal () throws IloException {
        return this.cplex.getStatus().equals(IloCplex.Status.Optimal);
    }
    
    public boolean isUnfeasible () throws IloException {
        return this.cplex.getStatus().equals(IloCplex.Status.Infeasible);
    }
    
    public long getActiveLeafCount () {
        return this.nodeHandler .activeLeafCount;
    }
}
