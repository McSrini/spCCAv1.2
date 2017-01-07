/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_2.cplex.callbacks;

import static ca.mcmaster.spccav1_2.Constants.FARMING_PHASE;
import static ca.mcmaster.spccav1_2.Constants.ONE;
import static ca.mcmaster.spccav1_2.Constants.TOTAL_LEAFS_IN_SOLUTION_TREE;
import static ca.mcmaster.spccav1_2.Constants.ZERO;
import ca.mcmaster.spccav1_2.cca.IndexTree;
import ca.mcmaster.spccav1_2.cplex.datatypes.NodeAttachment;
import ilog.concert.IloException;
import ilog.cplex.IloCplex.NodeCallback;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class ReconstructionNodehandler extends NodeCallback{
    
    private  double bestKnownSOlutionValue ;
    
    //map of new node, to old node ID
    private Map<String, String> newToOldMap = new HashMap<String, String>();
    //map of old  node, to new node ID, for every old node whose kids needs to be created
    private Map<String, String> oldToNewMap = new HashMap<String, String>();
    
    public IndexTree indexTree;    
    
    public ReconstructionNodehandler( double bestKnownSOlutionValue, Map<String, String> newToOldMap, Map<String, String> oldToNewMap) {
        this.bestKnownSOlutionValue =    bestKnownSOlutionValue;
        this.oldToNewMap =oldToNewMap;
        this.newToOldMap =newToOldMap;
    }


    @Override
    protected void main() throws IloException {
        if(getNremainingNodes64()> ZERO){
                           
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            NodeAttachment nodeData = (NodeAttachment) getNodeData(ZERO);
            if (nodeData==null ) { //it will be null for subtree root
               
                nodeData=new NodeAttachment (      );  
                setNodeData(ZERO,nodeData);                
                
            } 
            
            //always redirect to an old node included in the oldToNewMap keyset
            //if no such active leaf exists   , then merge is complete            
            
            //first check the default node slection
            String selectedNodeID = getNodeId(ZERO).toString(); 
            String oldNodeId = newToOldMap.get(selectedNodeID);
                    
            if (oldNodeId==null ) {
                
                //try to find an active leaf that corresponds to an old node
                //if no such active leaf exists, migration is complete because:
                //  1) all old nodes have been branched upon , OR
                //  2) some old nodes got resolved, so their descendents will never be created or branched upon
                //
                long selectedIndex =  checkMergeCompletion();
                if ( -ONE == selectedIndex) {                 
                    
                    //indexTree=new IndexTree(getAllLeafs());
                    
                    //stop  , migration is complete
                    abort();
                } else {
                    //select this node
                    selectNode( selectedIndex);
                }
                
            }else {
                //do nothing, take CPLEX's default node selection
            }
        }
    }
    
    private List<NodeAttachment> getAllLeafs () throws IloException {
        List<NodeAttachment> allLeafs = new ArrayList<NodeAttachment>();
        long numLeafs = getNremainingNodes64();
        for (int index = ZERO ; index < numLeafs; index ++){
            allLeafs.add((NodeAttachment)getNodeData(index) );
             
        }
        
        
        return allLeafs;
    }
    
    private long checkMergeCompletion() throws IloException {
        long selectedIndex = -ONE;
                        
        //pick up any active leaf, which  corresponds to an old node
       
        for (long index = ZERO; index<getNremainingNodes64(); index ++ ) {
            String newNodeId=getNodeId(index).toString();
            if ( newToOldMap.get(newNodeId) !=null) {
                selectedIndex=index;
                break;
                //we have found a candidate node
            }
        }
          
        
        return selectedIndex;
    }
    
}
