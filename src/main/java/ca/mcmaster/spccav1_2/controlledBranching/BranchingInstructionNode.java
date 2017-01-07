/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_2.controlledBranching;

import static ca.mcmaster.spccav1_2.Constants.ZERO;
import ca.mcmaster.spccav1_2.cplex.datatypes.BranchingInstruction;
import java.util.*;

/**
 *
 * @author tamvadss
 */
public class BranchingInstructionNode {
        
    public String nodeID;
    //up to 2 instructions, and the corresponding child nodes. 
    //The 1st instruction lets you fork to the first child, and the 2nd one to the 2nd child
    public List<BranchingInstruction>  branchingInstructionList = new ArrayList<BranchingInstruction>();
    public List<BranchingInstructionNode>  childList = new ArrayList<BranchingInstructionNode>();
    
    /*public BranchingInstructionNode (String nodeID,List<BranchingInstruction>  branchingInstructionList, List<BranchingInstructionNode>  childList ){
        this.branchingInstructionList=branchingInstructionList;
        this.childList=childList;
        this.nodeID=nodeID;
    }*/
    public BranchingInstructionNode (String nodeID  ){
       this.nodeID=nodeID;
    }
        
    public void createChild  (BranchingInstructionNode childNode, BranchingInstruction instruction) {
        branchingInstructionList.add( instruction );
        childList.add(childNode);
    }
    
    public boolean isLeaf() {
        return branchingInstructionList.size()==ZERO;
    }
}
