/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_2.controlledBranching;

import ca.mcmaster.spccav1_2.cca.IndexNode;
import ca.mcmaster.spccav1_2.cplex.datatypes.NodeAttachment;

import static ca.mcmaster.spccav1_2.Constants.*;
import ca.mcmaster.spccav1_2.cca.IndexTree;
import ca.mcmaster.spccav1_2.cplex.datatypes.BranchingInstruction;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
/**
 *
 * @author tamvadss
 * 
 * uses the in memory solution tree to populate CB metrics, generate the CB instruction tree, and then clears the metrics
 * 
 */
public class CBInstructionGenerator {
    
    private static Logger logger=Logger.getLogger(CBInstructionGenerator.class);
           
    private IndexNode ccaNode;
            
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+ CBInstructionGenerator.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
        }
          
    }
    
    
    public CBInstructionGenerator (IndexNode ccaNode){
        this.ccaNode= ccaNode;
    }
    
    //call this method to get the instructions that can be sent to the new partition
    public   BranchingInstructionTree getBranchingInstructionTree( List<String> nodesChosenForfarming, boolean stopBuildingTreeAtCCA){
        
        //prepare ref counts and skip counts
        this.updateDescendantRefCounts(nodesChosenForfarming);
        this.updateSkipCounts(nodesChosenForfarming);
             
        //create the instruction tree
        BranchingInstructionNode rootNode = new BranchingInstructionNode (MINUS_ONE_STRING);
        BranchingInstructionTree instructionTree = new BranchingInstructionTree(rootNode);
        
        //first task is to branch directly to the CCA node
        if (!ccaNode.nodeID.equals(MINUS_ONE_STRING )) 
            instructionTree.createChild( rootNode.nodeID, new BranchingInstructionNode(ccaNode.nodeID),  mergeBranchingInstructions(  ccaNode.cumulativeBranchingInstructions));
        
        if (!stopBuildingTreeAtCCA){
            //now build the instruction tree under the CCA node
            buildBranchingInstructionTree( IndexTree.solutionTreeNodeMap.get(ccaNode.nodeID),   instructionTree);
        }
        
        //we have the instruction tree, clear counts
        //reset binary tree ref counts and skip counts
        this.clearRefCountsAndSkipCounts(nodesChosenForfarming);
        logger.debug ("Printing Instruction tree\n"+instructionTree ) ;
            
        return instructionTree;
    }
    
    //use pre-order traversal of this binary tree to generate the instruction tree
    //Method has some code duplication and should be cleaned up
    //
    // 
    private   void buildBranchingInstructionTree(NodeAttachment  subtreeRoot, BranchingInstructionTree instructionTree){
        
        //here is the logic:
        //if a node has 2 or fewer kids under it, create the kids and no need for recursion.
        //Else if the node has 1 kid on one side, create the kid, and the immididate kid on the other side, then recursion on the other side
        //If 2 or more kids on both sides, then create immidiate kids on both sides, and recursion on both sides
        
        int leftSideSize = subtreeRoot.cbMetrics.chosenLeafsThatAreLeftDescendants.size() ;
        int rightSideSize = subtreeRoot.cbMetrics.chosenLeafsThatAreRightDescendants.size() ;
        
        logger.debug("building Branching Instruction Tree " + subtreeRoot.nodeID);
        
        if (leftSideSize< TWO && rightSideSize<TWO) {
            if(leftSideSize==ONE){                 
                //create left child
                String leftChildNodeID=subtreeRoot.cbMetrics.chosenLeafsThatAreLeftDescendants.get(ZERO);
                instructionTree.createChild( subtreeRoot.nodeID, 
                                             new BranchingInstructionNode( leftChildNodeID),   
                                             mergeBranchingInstructions(IndexTree.getCumulativeBranchingInstructions( leftChildNodeID , subtreeRoot.nodeID )));                
            }
            if(rightSideSize==ONE){
                //create right child
                String rightChildNodeID=subtreeRoot.cbMetrics.chosenLeafsThatAreRightDescendants.get(ZERO);
                instructionTree.createChild( subtreeRoot.nodeID, 
                                             new BranchingInstructionNode( rightChildNodeID),   
                                             mergeBranchingInstructions(IndexTree.getCumulativeBranchingInstructions( rightChildNodeID , subtreeRoot.nodeID )));
            }
            /*
            if(leftSideSize==TWO){
                //create left child
                String leftChildNodeID=subtreeRoot.cbMetrics.chosenLeafsThatAreLeftDescendants.get(ZERO);
                instructionTree.createChild( subtreeRoot.nodeID, 
                                             new BranchingInstructionNode( leftChildNodeID),   
                                             mergeBranchingInstructions(IndexTree.getCumulativeBranchingInstructions( leftChildNodeID , subtreeRoot.nodeID )));  
                //create the other child too, which will end up on the right side of this subtree root
                leftChildNodeID=subtreeRoot.cbMetrics.chosenLeafsThatAreLeftDescendants.get(ONE);
                instructionTree.createChild( subtreeRoot.nodeID, 
                                             new BranchingInstructionNode( leftChildNodeID),   
                                             mergeBranchingInstructions(IndexTree.getCumulativeBranchingInstructions( leftChildNodeID , subtreeRoot.nodeID ))); 
            }
            if(rightSideSize==TWO){
                //create right child
                String rightChildNodeID=subtreeRoot.cbMetrics.chosenLeafsThatAreRightDescendants.get(ZERO);
                instructionTree.createChild( subtreeRoot.nodeID, 
                                             new BranchingInstructionNode( rightChildNodeID),   
                                             mergeBranchingInstructions(IndexTree.getCumulativeBranchingInstructions( rightChildNodeID , subtreeRoot.nodeID )));
                //create the other child too
                rightChildNodeID=subtreeRoot.cbMetrics.chosenLeafsThatAreRightDescendants.get(ONE);
                instructionTree.createChild( subtreeRoot.nodeID, 
                                             new BranchingInstructionNode( rightChildNodeID),   
                                             mergeBranchingInstructions(IndexTree.getCumulativeBranchingInstructions( rightChildNodeID , subtreeRoot.nodeID )));
            }*/
        }
        if (rightSideSize==ONE && leftSideSize>=TWO) {
                //create right child
                String rightChildNodeID=subtreeRoot.cbMetrics.chosenLeafsThatAreRightDescendants.get(ZERO);
                instructionTree.createChild( subtreeRoot.nodeID, 
                                             new BranchingInstructionNode( rightChildNodeID),   
                                             mergeBranchingInstructions(IndexTree.getCumulativeBranchingInstructions( rightChildNodeID , subtreeRoot.nodeID )));
                
                //create   left kid after any skipping , and make recursive call to left side
                String leftChildNodeID = this.createNextLeftChildAfterSkipping(subtreeRoot, instructionTree);
                
                //recursive call
                buildBranchingInstructionTree(  IndexTree.solutionTreeNodeMap.get( leftChildNodeID)  ,   instructionTree);
                
        } 
        
        if (leftSideSize==ONE && rightSideSize>=TWO) {
                //create left child
                String leftChildNodeID=subtreeRoot.cbMetrics.chosenLeafsThatAreLeftDescendants.get(ZERO);
                instructionTree.createChild( subtreeRoot.nodeID, 
                                             new BranchingInstructionNode( leftChildNodeID),   
                                             mergeBranchingInstructions(IndexTree.getCumulativeBranchingInstructions( leftChildNodeID , subtreeRoot.nodeID ))); 
                
                //create right kid after any skipping , and make recursive call to right side
                String rightChildNodeID = this.createNextRightChildAfterSkipping(subtreeRoot, instructionTree);
                
                //recursive call
                buildBranchingInstructionTree(  IndexTree.solutionTreeNodeMap.get( rightChildNodeID )  ,   instructionTree);
                
        } 
        
        if (rightSideSize>=TWO && leftSideSize>=TWO) {
            //both side have counts   >=2
            
            //create immidiate kids on both sides, and make recursive calls on both sides. Take skipping into account.
            
            //create right kid and make recursive call to right side
            String rightChildNodeID = this.createNextRightChildAfterSkipping(subtreeRoot, instructionTree);             
            //recursive call
            buildBranchingInstructionTree(  IndexTree.solutionTreeNodeMap.get( rightChildNodeID )  ,   instructionTree);
             
            //create   left kid and make recursive call to left side
            String leftChildNodeID=  this.createNextLeftChildAfterSkipping(subtreeRoot, instructionTree);
            //recursive call
            buildBranchingInstructionTree(  IndexTree.solutionTreeNodeMap.get( leftChildNodeID)  ,   instructionTree);
            
        }
        
    }
    
    private String createNextLeftChildAfterSkipping(NodeAttachment  subtreeRoot, BranchingInstructionTree instructionTree){
        
        NodeAttachment thisChild= IndexTree.solutionTreeNodeMap.get(  subtreeRoot.leftChildNodeID);
        
        BranchingInstruction compoundInstr = new BranchingInstruction();
        compoundInstr.merge(subtreeRoot.branchingInstructionForLeftChild);
        
        for (int index = ZERO;  index < subtreeRoot.cbMetrics.leftSideSkipCount; index++){

            boolean isSkipDirectionLeft =  thisChild.cbMetrics.chosenLeafsThatAreRightDescendants.size()==ZERO;
            if (isSkipDirectionLeft) {
                //skip one node to the left
                compoundInstr .merge(thisChild.branchingInstructionForLeftChild);
                thisChild = IndexTree.solutionTreeNodeMap.get( thisChild .leftChildNodeID);
            }else {
                //skip to the right
                //note that first skip is always to the right when rightSideSkipCount>0
                compoundInstr .merge(thisChild.branchingInstructionForRightChild);
                thisChild = IndexTree.solutionTreeNodeMap.get( thisChild .rightChildNodeID);
            }                   
        }
        
        instructionTree.createChild( subtreeRoot.nodeID, 
                                         new BranchingInstructionNode( thisChild.nodeID ), compoundInstr ); 
        
        return thisChild.nodeID;
        
    }
    
    private String createNextRightChildAfterSkipping(NodeAttachment  subtreeRoot, BranchingInstructionTree instructionTree){
        
        NodeAttachment thisChild= IndexTree.solutionTreeNodeMap.get(  subtreeRoot.rightChildNodeID);
        
        BranchingInstruction compoundInstr = new BranchingInstruction();
        compoundInstr.merge(subtreeRoot.branchingInstructionForRightChild);
        
        for (int index = ZERO;  index < subtreeRoot.cbMetrics.rightSideSkipCount; index++){

            boolean isSkipDirectionLeft =  thisChild.cbMetrics.chosenLeafsThatAreRightDescendants.size()==ZERO;
            if (isSkipDirectionLeft) {
                //skip one node to the left
                compoundInstr .merge(thisChild.branchingInstructionForLeftChild);
                thisChild = IndexTree.solutionTreeNodeMap.get( thisChild .leftChildNodeID);
            }else {
                //skip to the right
                //note that first skip is always to the right when rightSideSkipCount>0
                compoundInstr .merge(thisChild.branchingInstructionForRightChild);
                thisChild = IndexTree.solutionTreeNodeMap.get( thisChild .rightChildNodeID);
            }                   
        }
        
        instructionTree.createChild( subtreeRoot.nodeID, 
                                         new BranchingInstructionNode( thisChild.nodeID ), compoundInstr ); 
        
        return thisChild.nodeID;
        
    }
    
    private  BranchingInstruction mergeBranchingInstructions(List <BranchingInstruction> cumulativeBranchingInstructions ) {
        BranchingInstruction result = new BranchingInstruction ();
                
        for (int index = ZERO; index < cumulativeBranchingInstructions.size() ; index ++) {
            result.merge(  cumulativeBranchingInstructions.get(index));
        }
        return result;
    }
    
    //for every leaf chosen for migration, send its node ID up
    private void updateDescendantRefCounts (List<String> leafsChosenForfarming   ){
        for (String nodeID :leafsChosenForfarming ){
            
            //get the node              
            NodeAttachment node = IndexTree.solutionTreeNodeMap.get( nodeID);
            //get the parent node
            NodeAttachment parentNode = node.parentData;
           
            //this node, and each of its parents, must send up the node ID , and 
            //wheteher they are on the left or right side of their parent
            while (!node.nodeID .equals(  this.ccaNode.nodeID)){
                
                String currentNodeID = node.nodeID;
                                
                //send up the node id sum
                if (parentNode.leftChildNodeID. equals(currentNodeID )) {
                    //  left child    
                    parentNode.cbMetrics.chosenLeafsThatAreLeftDescendants.add(nodeID);
                }else {
                    //  right child   
                    parentNode.cbMetrics.chosenLeafsThatAreRightDescendants.add(nodeID);
                }
               
                //climb up
                node = parentNode;
                parentNode=node.parentData;                 
            }
           
        }
    }
    
    //  assign every node its left and right skip count
    private void updateSkipCounts(List<String> leafsChosenForfarming ){
        //
        for (String nodeIDofFarmedLeaf :leafsChosenForfarming ){            
            //get the node              
            NodeAttachment currentNode = IndexTree.solutionTreeNodeMap.get( nodeIDofFarmedLeaf);           
            NodeAttachment parentNode = currentNode.parentData;
            
            logger.info("updateSkipCounts with current node " + currentNode.nodeID + " having parent "+ parentNode.nodeID);
                        
            //this node, and each of its parents, must do the following 
            //   check if self can be skipped over, i.e. if self's refcounts are like (N>2, 0) or (0, N>2)
            //if yes, inform parent of direction and cumulative skip count
            
            while (! currentNode.nodeID.equals( this.ccaNode.nodeID)){
                
                String currentNodeID = currentNode.nodeID;
                    
                boolean canSelfBeSkippedOver = currentNode.cbMetrics.chosenLeafsThatAreLeftDescendants.size() ==ZERO && 
                                               currentNode.cbMetrics.chosenLeafsThatAreRightDescendants.size() >= TWO;
                canSelfBeSkippedOver = canSelfBeSkippedOver ||  
                                       (currentNode.cbMetrics. chosenLeafsThatAreRightDescendants .size() ==ZERO && 
                                       currentNode.cbMetrics.chosenLeafsThatAreLeftDescendants.size() >= TWO);

                Boolean amITheLeftChild = parentNode.leftChildNodeID.equals(currentNodeID );

                if (canSelfBeSkippedOver) {

                    //check if I have a skip count that I recieved from either of my 2 kids
                    //Recall that , since I am skippable, at most one kid could have sent me a skip count
                    int mySkipCount = Math.max( currentNode.cbMetrics.leftSideSkipCount , currentNode.cbMetrics.rightSideSkipCount );

                    //now send the parent the cumulative skip count 
                    if (amITheLeftChild) {
                        parentNode.cbMetrics.leftSideSkipCount= ONE + mySkipCount;
                    }  else {
                        parentNode.cbMetrics.rightSideSkipCount  = ONE + mySkipCount;
                    }

                } else {
                    //send 0 skip count to parent
                    if (amITheLeftChild) parentNode.cbMetrics.leftSideSkipCount=ZERO; else parentNode.cbMetrics.rightSideSkipCount=ZERO;
                } 
               
                currentNode = parentNode;
                parentNode=currentNode.parentData;                 
            }
           
        }
    }
    
    //reset the counts after generating branching instructions
    private void clearRefCountsAndSkipCounts(List<String> leafsChosenForfarming ){
        
        //from every chosen leaf, clear counts for every ancestor as we move up
        //can stop at first ancestor who is already cleared        
        
        for (String nodeIDofFarmedLeaf :leafsChosenForfarming ){            
            //get the node              
            NodeAttachment currentNode = IndexTree.solutionTreeNodeMap.get( nodeIDofFarmedLeaf);           
            NodeAttachment parentNode = currentNode.parentData;
            
            logger.info(" clear counts with current node " + currentNode.nodeID + " having parent "+ parentNode.nodeID);
                                     
            while (! currentNode.nodeID.equals( this.ccaNode.nodeID)){
                
                String currentNodeID = currentNode.nodeID;
                    
                //clear parent if not already cleared, else break
                if (!parentNode.cbMetrics.isCleared()) {
                    parentNode.cbMetrics.clear();
                }else {
                    //we can go to next leaf - this leaf is done
                    break;
                }

                currentNode = parentNode;
                parentNode=currentNode.parentData;                 
            }
           
        }
    }
    
}
