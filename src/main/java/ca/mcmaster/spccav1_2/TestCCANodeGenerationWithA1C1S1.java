/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_2;

import static ca.mcmaster.spccav1_2.Constants.BILLION;
import static ca.mcmaster.spccav1_2.Constants.FARMING_PHASE;
import static ca.mcmaster.spccav1_2.Constants.LOG_FILE_EXTENSION;
import static ca.mcmaster.spccav1_2.Constants.*;
import static ca.mcmaster.spccav1_2.Constants.MPS_FILE_ON_DISK;
import static ca.mcmaster.spccav1_2.Constants.ONE;
import static ca.mcmaster.spccav1_2.Constants.ZERO;
import ca.mcmaster.spccav1_2.cca.IndexNode;
import ca.mcmaster.spccav1_2.controlledBranching.BranchingInstructionTree;
import ca.mcmaster.spccav1_2.controlledBranching.CBInstructionGenerator;
import ca.mcmaster.spccav1_2.cplex.ActiveSubtree;
import ca.mcmaster.spccav1_2.cplex.datatypes.BranchingInstruction;
import ilog.cplex.IloCplex;
import java.io.File;
import static java.lang.System.exit;
import java.time.LocalDateTime;
import java.util.Arrays;
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
public class TestCCANodeGenerationWithA1C1S1 {
    
    private static  Logger logger = null;
       
    public static void main(String[] args) throws Exception {
     
        logger=Logger.getLogger(TestCCANodeGenerationWithA1C1S1.class);
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+TestCCANodeGenerationWithA1C1S1.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            exit(1);
        }
        
        
        
        BackTrack= true;
        
        
        IloCplex cplex= new IloCplex();   
        cplex.importModel(MPS_FILE_ON_DISK);
        ActiveSubtree activeSubtree = new ActiveSubtree(cplex, null) ;
        
        /*
        FARMING_PHASE= false;
        
        double cutoff =  BILLION;
        while (! isHaltFilePresent()){
            long nodeCount = activeSubtree.getNumNodesSolved();
            if (    activeSubtree.isOptimal()) break;
            activeSubtree.solveFor2Min();
            if (activeSubtree.isOptimal()) logger.info("M0 solution is "+activeSubtree.getSolution());
            logger.info("Number of nodes solved is = "+  (activeSubtree.getNumNodesSolved()-nodeCount));
            cutoff = Math.min ( cutoff,  activeSubtree.getSolution());
            logger.info("BEST Known solution is " +cutoff);
            
            logger.info("Initial activeSubtree best reamining obj val "+ activeSubtree.getBestObjValue()); 
             
        }
         
        exit(2);*/
         
        //solve and farm few nodes out
        activeSubtree.solve();
        List<IndexNode> ccaNodes = activeSubtree.getCCANodes();
          
        for (IndexNode node: ccaNodes){
            logger.info(node);
        }
        
        
        //List <String > nodesChosenForfarming = Arrays.asList("Node19", "Node27" , "Node20" );
        List <String > nodesChosenForfarming = Arrays.asList("Node14", "Node32" , "Node34" );
      
        IndexNode testNode = activeSubtree.getCCANode( nodesChosenForfarming) ;
        logger.info(testNode);
        
        CBInstructionGenerator instructionGenerator= new CBInstructionGenerator(testNode);
        BranchingInstructionTree instructionTree = instructionGenerator.getBranchingInstructionTree(  nodesChosenForfarming,   true);
        
        logger.info("\nInstruction tree \n" + instructionTree);
        
        instructionGenerator= new CBInstructionGenerator(testNode);
        instructionTree = instructionGenerator.getBranchingInstructionTree(  nodesChosenForfarming,   false);
        
        logger.info("\nInstruction tree full\n" + instructionTree);
        
        exit(5);
         
                
        //select 1 CCA, and continue solution
        
        FARMING_PHASE= false;
        
        IndexNode  selectedCCANode = ccaNodes.get(ONE);
        logger.info("Selected CCA Node is  - " + selectedCCANode);
        activeSubtree.pruneCCANode(selectedCCANode);
        
        //construct new active subtree from cca node
        IloCplex cplexNew= new IloCplex();   
        cplexNew.importModel(MPS_FILE_ON_DISK);
        //cplexNew.exportModel("F:\\temporary files here\\logs\\testing\\msc98-ip.lp");
        ActiveSubtree activeSubtreeNew = new ActiveSubtree(cplexNew, selectedCCANode  ) ;
        // cplexNew.exportModel("F:\\temporary files here\\logs\\testing\\msc98-ip.node65.lp");
        
        //loop solve both sub trees
        logger.info("Starting full solve at " + LocalDateTime.now());
        double cutoff =  IS_MAXIMIZATION ? -BILLION:BILLION;
        while (! isHaltFilePresent()){
            
            cutoff = Math.min ( cutoff, Math.min(activeSubtreeNew.getSolution(), activeSubtree.getSolution()));
            logger.info("BEST Known solution is " +cutoff);
            
            logger.info("Initial activeSubtree best reamining obj val "+ activeSubtree.getBestObjValue() + " count of remaining leafs "+activeSubtree.getActiveLeafCount()
            + " branches "+ activeSubtree.getNumBranches()); 
            logger.info("New activeSubtree best reamining obj val "+ activeSubtreeNew.getBestObjValue()+ " count of remaining leafs "+activeSubtreeNew.getActiveLeafCount()
            + " branches "+ activeSubtreeNew.getNumBranches()); 
            
            long nodeCountOldtree = activeSubtree.getNumNodesSolved();
            long nodeCountNEwTree = activeSubtreeNew.getNumNodesSolved();
            
            activeSubtreeNew.setUpperCutoff(cutoff) ;
            activeSubtree .setUpperCutoff(cutoff) ;
           
            if (! activeSubtreeNew.isOptimal()) activeSubtreeNew.solveFor2Min (); else  logger.info("Completed activeSubtree new" );
            
            if (activeSubtreeNew.isUnfeasible() && activeSubtreeNew.getNumBranches()==ZERO){
                logger.info(" **** "+ activeSubtreeNew.getNumBranches());
                exit(3);
            }
            
            if (! activeSubtree.isOptimal()) activeSubtree.solveFor2Min (); else  logger.info("Completed activeSubtree");
            
            logger.info("Number of oldtree nodes solved is = "+  (activeSubtree.getNumNodesSolved()-nodeCountOldtree));
            logger.info("Number of newtree nodes solved is = "+  (activeSubtreeNew.getNumNodesSolved()-nodeCountNEwTree));
            
            if (  activeSubtreeNew.isOptimal()  && activeSubtree.isOptimal()) break;
        }
       
        if (activeSubtree.isOptimal()) logger.info("M0 solution is "+activeSubtree.getSolution());
        if (activeSubtreeNew.isOptimal()) logger.info("M1 completed solution is "+activeSubtreeNew.getSolution());
        
        
        logger.info("Completed full solve at " + LocalDateTime.now()); 
           
    }
    
    
    private static boolean isHaltFilePresent (){
        File file = new File("F:\\temporary files here\\haltfile.txt");
         
        return file.exists();
    }
    
}
