/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_2.cca;

import static ca.mcmaster.spccav1_2.Driver.*;
import static ca.mcmaster.spccav1_2.Driver.ZERO;
import ca.mcmaster.spccav1_2.cplex.datatypes.BranchingInstruction;
import ca.mcmaster.spccav1_2.cplex.datatypes.NodeAttachment;
import java.util.*;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class IndexTree {
    
    //every time an index tree is constructed on a partition, we prepare these maps which serve as indices
    public static Map<String,NodeAttachment > solutionTreeNodeMap = new HashMap<String,NodeAttachment> ();
    public static Map<String, String> leftChildMap = new HashMap<String, String>();
    public static Map<String, String> rightChildMap = new HashMap<String, String>();
    public static Map<String, String> parentsMap = new HashMap<String, String>();
     
    private IndexNode subtreeRoot ;
    
    private List<NodeAttachment> allLeafNodes;
    
    public IndexTree (List<NodeAttachment> allLeafNodes ) {
        this.allLeafNodes= allLeafNodes;
        prepareIndexMaps();
        
        subtreeRoot = new IndexNode(   allLeafNodes, ZERO, EMPTY_STRING+-ONE, null);
        
    }
    
    public void splitToCCA ( ) {
        subtreeRoot.splitToCCA();
    }
    
    //return the CCA subtrees found  
    public List<IndexNode> getCCANodes () {
        List<IndexNode> ccaList =  this.subtreeRoot.getCCATrees();
        for (IndexNode node: ccaList) {
            setNumberOfNodeLPsRequiredToConstructAllLeafs(node);
        }
        return ccaList;
    }
    
   
    //this number , when multiplied by average node LP solve time, should be much less than expected solution time of CCA node
    private void setNumberOfNodeLPsRequiredToConstructAllLeafs(IndexNode ccaNode){
        //move up from every leaf towards CCA node, and count the number of 
        //unique node IDs encountered along the way, including the CCA node itself
        //
        //Be careful of the case when the CCA node is itself a leaf, in which case no need to solve any node LPs
        
        ccaNode.numNodeLPsToSolveToArriveAtLeafs = ZERO ;
        
        if (ccaNode.leafNodesToTheLeft.size() + ccaNode.leafNodesToTheRight.size() > ZERO){
            
            Map<String, Integer> uniqueNodeMap = new HashMap<String, Integer>();
            
            for (NodeAttachment node : ccaNode.leafNodesToTheLeft) {
                NodeAttachment thisNode = node;
                NodeAttachment parentNode = node.parentData;
                while (!thisNode.nodeID.equals( ccaNode.nodeID)){
                    uniqueNodeMap.put(parentNode.nodeID, ONE);
                    thisNode = parentNode;
                    parentNode = thisNode.parentData;
                }
            }
            for (NodeAttachment node : ccaNode.leafNodesToTheRight) {
                NodeAttachment thisNode = node;
                NodeAttachment parentNode = node.parentData;
                while (!thisNode.nodeID.equals( ccaNode.nodeID)){
                    uniqueNodeMap.put(parentNode.nodeID, ONE);
                    thisNode = parentNode;
                    parentNode = thisNode.parentData;
                }
            }
             
            ccaNode.numNodeLPsToSolveToArriveAtLeafs =uniqueNodeMap.keySet().size();
        } 
        
    }
    
    private   void prepareIndexMaps () {
        
        solutionTreeNodeMap.clear();
        leftChildMap.clear();
        rightChildMap.clear();
        parentsMap.clear();
        
        NodeAttachment subtreeRoot =null;
        
        for (NodeAttachment leaf :allLeafNodes ){
            //climb all the way up to subtree root, recording parent ID and NodeAttachment
            
            NodeAttachment thisNode = leaf;
            NodeAttachment parent = leaf.parentData;
            while (parent !=null) {
                solutionTreeNodeMap.put(thisNode.nodeID, thisNode);
                parentsMap.put(thisNode.nodeID, parent.nodeID);
                
                if (parent.leftChildNodeID.equals( thisNode.nodeID)){
                    leftChildMap.put( parent.nodeID, thisNode.nodeID );
                } else {
                    rightChildMap.put( parent.nodeID, thisNode.nodeID );
                }
                
                //climb up
                thisNode = parent;
                parent = parent.parentData;
            }
            
            //parent is null
            subtreeRoot = thisNode;
        }
        
        solutionTreeNodeMap.put(subtreeRoot.nodeID, subtreeRoot );
        parentsMap.put(subtreeRoot.nodeID, null);
            
    }
    
}
