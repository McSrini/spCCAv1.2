/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_2.cplex.callbacks;
 
import static ca.mcmaster.spccav1_2.Constants.*;
import ca.mcmaster.spccav1_2.cplex.datatypes.*;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BranchCallback;
import ilog.cplex.IloCplex.BranchDirection;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class BranchHandler extends BranchCallback {
    
    private static Logger logger=Logger.getLogger(BranchHandler.class);
     
    
    public List<String> pruneList = new ArrayList<String>();
    
    public double bestReaminingObjValue = IS_MAXIMIZATION ? -BILLION : BILLION;
    public double cutoff = IS_MAXIMIZATION ? -BILLION : BILLION;
    
    public int numBranches = ZERO;
    
    static {
        logger.setLevel(Level.OFF);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+BranchHandler.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            exit(1);
        }
          
    }

    @Override
    protected void main() throws IloException {
        
        if ( getNbranches()> 0 ){  
            
            numBranches +=getNbranches();
                        
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            NodeAttachment nodeData = (NodeAttachment) getNodeData();
            if (nodeData==null ) { //it will be null for subtree root
               
                nodeData=new NodeAttachment (      );  
                setNodeData(nodeData);                
                
            } 
            
            if (pruneList.size()> ZERO && pruneList.contains(nodeData.nodeID) ) {
                //
                pruneList.remove( nodeData.nodeID);
                prune();
            } else if (IS_MAXIMIZATION && getObjValue() < cutoff){
                prune();
            }else if (!IS_MAXIMIZATION && getObjValue() > cutoff){
                prune();
            }else {
                
                //get the branches about to be created
                IloNumVar[][] vars = new IloNumVar[2][] ;
                double[ ][] bounds = new double[2 ][];
                BranchDirection[ ][]  dirs = new  IloCplex.BranchDirection[ 2][];
                getBranches(  vars, bounds, dirs);

                //now allow  both kids to spawn
                for (int childNum = ZERO ;childNum<getNbranches();  childNum++) {   
                    
                    //apply the bound changes specific to this child
                    BranchingInstruction bi = createBranchingInstruction(   dirs[childNum], bounds[childNum], vars[childNum] );
                    
                    NodeAttachment thisChild  =  createChildNode( nodeData,   childNum ); 

                    IloCplex.NodeId nodeid = makeBranch(childNum,thisChild );
                    thisChild.nodeID =nodeid.toString();
                    
                    logger.debug(" Node "+nodeData.nodeID + " created child "+  thisChild.nodeID + " varname " +
                           vars[childNum][0].getName() + " bound " + bounds[childNum][0] +   (dirs[childNum][0].equals( BranchDirection.Down) ? " U":" L") ) ;
                    
                    if (childNum == ZERO) {
                        //update left child info
                        nodeData.leftChildNodeID =thisChild.nodeID;
                        nodeData.branchingInstructionForLeftChild =bi;
                    }else {
                        nodeData.rightChildNodeID =thisChild.nodeID;
                        nodeData.branchingInstructionForRightChild =bi;
                    }

                }//end for 2 kids
                
            }
            
            this.bestReaminingObjValue = getBestObjValue();
              
        }
          
    }
    
    private BranchingInstruction createBranchingInstruction(BranchDirection[ ]  dirs,double[ ] bounds, IloNumVar[] vars ) {
        return new  BranchingInstruction(  getVarnames (  vars),   getVarDirs(dirs),   bounds);
    }
    
    private NodeAttachment createChildNode (NodeAttachment parentNodeData,int childNum ){
        NodeAttachment thisChild  = new NodeAttachment (); 
        
        thisChild.parentData = parentNodeData;
        thisChild.depthFromSubtreeRoot=parentNodeData.depthFromSubtreeRoot + ONE;
        
        //thisChild.isMigrateable= rand.nextBoolean() || rand.nextBoolean();
     
        //0 or false indicates L
        thisChild.isRightChildCumulative.addAll( parentNodeData.isRightChildCumulative);
        thisChild.isRightChildCumulative.add(( ONE==childNum));
         
        return     thisChild;    
    }
    
    private String[] getVarnames (IloNumVar[] vars) {
        String[] varnames = new  String[vars.length];
        
        int index = ZERO;
        for (IloNumVar var : vars) {
            varnames[index ++] = var.getName();
        }
        return varnames;
    }
    
    private Boolean[] getVarDirs (BranchDirection[ ]  dirs) {
        Boolean[] vardirs = new  Boolean[dirs.length];
        
        int index = ZERO;
        for (BranchDirection dir : dirs) {
            vardirs[index ++] = dir.equals( BranchDirection.Down);
        }
        return vardirs;
    }
    
}
