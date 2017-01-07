/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_2.controlledBranching;
 
import static ca.mcmaster.spccav1_2.Constants.*;
import ca.mcmaster.spccav1_2.cplex.datatypes.BranchingInstruction;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class BranchingInstructionTree {
    
    private static Logger logger=Logger.getLogger(BranchingInstructionTree.class);
       
    public BranchingInstructionNode rootNode;
    
    //index to every node   
    public Map <String, BranchingInstructionNode> nodeMap = new LinkedHashMap <String, BranchingInstructionNode>();
    
    static {
        logger.setLevel(Level.OFF);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+BranchingInstructionTree.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
        }
          
    }
    
    public BranchingInstructionTree (BranchingInstructionNode rootNode) {
        this. rootNode=  rootNode;
        nodeMap.put (rootNode.nodeID, rootNode);
    }
    
    //create child for node with nodeID
    public void createChild  (String nodeID, BranchingInstructionNode childNode, BranchingInstruction instruction) {
        BranchingInstructionNode node = nodeMap.get(nodeID);
        node.branchingInstructionList.add( instruction );
        node.childList.add(childNode);
        //update our index
        nodeMap.put(childNode.nodeID, childNode);
    }
    
    public String toString() {
        String result="";
        //print node id, and its kids
        logger.info("Branching Instruction Tree ");
        for (Map.Entry  <String, BranchingInstructionNode> entry : nodeMap.entrySet()){
            BranchingInstructionNode bin = entry.getValue();
            result += bin;        
             result += "\n";
        }
        return result;
    }
    
}
