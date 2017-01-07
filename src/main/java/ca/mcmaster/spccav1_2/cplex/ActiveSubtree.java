/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_2.cplex;
   
import static ca.mcmaster.spccav1_2.Constants.*;
import ca.mcmaster.spccav1_2.cplex.utilities.UtilityLibrary;
import ca.mcmaster.spccav1_2.cca.IndexNode;
import ca.mcmaster.spccav1_2.cca.IndexTree;
import ca.mcmaster.spccav1_2.controlledBranching.BranchingInstructionNode;
import ca.mcmaster.spccav1_2.controlledBranching.BranchingInstructionTree;
import ca.mcmaster.spccav1_2.cplex.callbacks.*;
import ca.mcmaster.spccav1_2.cplex.datatypes.*;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map; 
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class ActiveSubtree {
    
    private static Logger logger=Logger.getLogger(ActiveSubtree.class);
        
    private IloCplex cplex   ;
    
    //this is the branch handler for the CPLEX object
    private BranchHandler branchHandler;
    private NodeHandler nodeHandler;
    
    public IndexTree indexTree=null;
    
    static {
        logger.setLevel(Level.WARN);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+ActiveSubtree.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
        }
          
    }
    
    public ActiveSubtree (  IloCplex cplex,  BranchingInstructionTree branchingInstructionTree, double bestKnownSolutionValue ) throws Exception{
        this.cplex=cplex;
        
        //construct the default handler sbut do not use them yet
        branchHandler = new BranchHandler(    );
        nodeHandler = new NodeHandler(    );       
         
        
        //map of new node, to old node ID
        Map<String, String> newToOldMap = new HashMap<String, String>();
        //map of old  node, to new node ID, for every old node whose kids needs to be created
        Map<String, String> oldToNewMap = new HashMap<String, String>();
        populateNewAndOldNodeIDMaps(newToOldMap, oldToNewMap,   branchingInstructionTree);
        
        //use the reconstruction handlers until merging is complete
        this.cplex.use(new ReconstructionBranchHandler(bestKnownSolutionValue, newToOldMap, oldToNewMap, branchingInstructionTree));
        this.cplex.use(new ReconstructionNodehandler(bestKnownSolutionValue, newToOldMap, oldToNewMap)); 
    }
           
    
    public ActiveSubtree (IloCplex cplex  , IndexNode selectedCCANode ) throws Exception{
        
        this.cplex=cplex;
        
        if (selectedCCANode!=null) {
            Map< String, Double >   lowerBounds= getLowerBounds(selectedCCANode.cumulativeBranchingInstructions);
            Map< String, Double >   upperBounds= getUpperBounds(selectedCCANode.cumulativeBranchingInstructions);
            UtilityLibrary.merge(cplex,  lowerBounds, upperBounds);
        } 
        
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
    
    public IndexNode getCCANode  (List<String> selectedLeafNodeIDs) {
        return this.indexTree.getCCANode(selectedLeafNodeIDs);
    }
    
  
    public void solve() throws IloException{
        if (BackTrack) cplex.setParam( IloCplex.Param.MIP.Strategy.Backtrack,  ZERO); 
        cplex.solve();
        this.indexTree = nodeHandler.indexTree;
    }
        
    public void solveFor2Min() throws IloException{
        cplex.setParam(IloCplex.Param.TimeLimit, TWO*SIXTY); 
        cplex.solve();     
    }
    
    //call this method to merge migratedLeafs
    public void solveTillLeafsMerged() throws IloException{
        cplex.solve();   
        //restore callbacks is this MIP will be solved further
        if (cplex.getStatus().equals(IloCplex.Status.Feasible)) setDefaultCallbacks();
    }
    
    //use this method to restore normal callbacks after leaf merge is complete
    public void setDefaultCallbacks() throws IloException{
        this.cplex.use(branchHandler);
        this.cplex.use(nodeHandler);    
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
    public void pruneCCANode (IndexNode  selectedCCANode) {
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
    

        
    public boolean isOptimal () throws IloException {
        return this.cplex.getStatus().equals(IloCplex.Status.Optimal);
    }
    
    public boolean isUnfeasible () throws IloException {
        return this.cplex.getStatus().equals(IloCplex.Status.Infeasible);
    }
    
    public long getActiveLeafCount () {
        return this.nodeHandler .activeLeafCount;
    }
    
        
    public static Map< String, Double >   getUpperBounds   (List <BranchingInstruction> cumulativeBranchingInstructions) {
        Map< String, Double > upperBounds = new HashMap < String, Double > ();
        
        for (BranchingInstruction bi: cumulativeBranchingInstructions){
            
            for (int index = ZERO ; index < bi.size(); index ++){
                if ( bi.isBranchDirectionDown.get(index)){
                    
                    logger.info("Upper bound Branching instruction is: " + bi);
            
                    String varName = bi.varNames.get(index);
                    double value = bi.varBounds.get(index);
                    if (upperBounds.containsKey(varName)) {
                        double existingValue = upperBounds.get( varName);
                        if (existingValue>value ) upperBounds.put(varName, value);
                    } else {
                        upperBounds.put(varName, value);
                    }
                }
            }
        }
        
        return  upperBounds ;
    }

    public static Map< String, Double >   getLowerBounds   (List <BranchingInstruction> cumulativeBranchingInstructions) {
        Map< String, Double > upperBounds = new HashMap < String, Double > ();
        
        for (BranchingInstruction bi: cumulativeBranchingInstructions){            
            
            for (int index = ZERO ; index < bi.size(); index ++){
                if ( ! bi.isBranchDirectionDown.get(index)){
                    
                    logger.info("Lower bound Branching instruction is: " + bi);
                    
                    String varName = bi.varNames.get(index);
                    double value = bi.varBounds.get(index);
                    if (upperBounds.containsKey(varName)) {
                        double existingValue = upperBounds.get( varName);
                        if (existingValue<value ) upperBounds.put(varName, value);
                    } else {
                        upperBounds.put(varName, value);
                    }
                }
            }            
        }
        
        return  upperBounds ;
    }
    
    // map of old  node, to new node ID, for every old node whose kids needs to be created
    //
    //map of new node, to old node ID
    private void populateNewAndOldNodeIDMaps (Map<String, String> newToOldMap,        Map<String, String> oldToNewMap, BranchingInstructionTree branchingInstructionTree ) {
        
        for (Map.Entry <String, BranchingInstructionNode> entry : branchingInstructionTree.nodeMap.entrySet()){
            String nodeID = entry.getKey();
            BranchingInstructionNode biNode = entry.getValue();
            if (!biNode.isLeaf()) oldToNewMap.put( nodeID, null );
        }
        
        //both trees start from original root
        newToOldMap.put (MINUS_ONE_STRING, MINUS_ONE_STRING);
        oldToNewMap.put (MINUS_ONE_STRING, MINUS_ONE_STRING);
        
    }

}
