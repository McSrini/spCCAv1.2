/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_2.cplex.datatypes;

import static ca.mcmaster.spccav1_2.Constants.*;
import ca.mcmaster.spccav1_2.controlledBranching.CBMetrics;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tamvadss
 */
public class NodeAttachment {
        
    //default for subtree root 
    public String nodeID = EMPTY_STRING + -ONE; 
    public int depthFromSubtreeRoot = ZERO;        
    
    public  NodeAttachment  parentData = null;
    
    
    //random for now, this will be determined by node metrics
    public boolean isMigrateable = true;
    
    public BranchingInstruction branchingInstructionForLeftChild = new BranchingInstruction();
    public BranchingInstruction branchingInstructionForRightChild = new BranchingInstruction();
    public String rightChildNodeID = null; 
    public String leftChildNodeID = null; 
     
    //we maintain this small list , so we can quickly calculate which children are to the left and which to the right
    //Example LLRL is the 3rd leaf from the left extreme
    //0 indicates L, which is the 0th child created by the branch handler, 1 indicares R i.e the 1st child
    public List<Boolean> isRightChildCumulative = new ArrayList <Boolean>();
                        
    //these metrics are used and cleared by controlled branching, when CB is invoked
    public CBMetrics cbMetrics = new CBMetrics();
 
    public String toString(){
        String result = EMPTY_STRING;
        result += "NodeID "+ nodeID;
        result += isMigrateable? " Mig":" Un";
        result += " ";
        for (Boolean isRight: isRightChildCumulative ) {
            result +=  isRight? "R":"L";
        }
          
        result += "\n";
        return result;
    }
    
    public boolean isLeaf() {
        return rightChildNodeID == null && leftChildNodeID == null; 
    }
  
}
