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
import ca.mcmaster.spccav1_2.cplex.datatypes.NodeAttachment;
import ilog.cplex.IloCplex;
import java.io.File;
import static java.lang.System.exit;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
public class TestCCANodeGenerationWithRDPLU {
    
    private static  Logger logger = null;
       
    public static void main(String[] args) throws Exception {
     
        logger=Logger.getLogger(TestCCANodeGenerationWithRDPLU.class);
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+TestCCANodeGenerationWithRDPLU.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(TEN*TEN);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
            
        } catch (Exception ex) {
            exit(1);
        }
        
        MPS_FILE_ON_DISK=   "F:\\temporary files here\\rd-rplusc-21.mps";
         
        
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
           
        
        CBInstructionGenerator instructionGenerator= new CBInstructionGenerator(ccaNodes.get(ZERO));
        
        List <String > nodesChosenForfarming = new ArrayList <String >();
        for (NodeAttachment atch : ccaNodes.get(ZERO).leafNodesToTheLeft) {
            nodesChosenForfarming.add (    atch.nodeID    );
        }
        for (NodeAttachment atch : ccaNodes.get(ZERO).leafNodesToTheRight){
            nodesChosenForfarming.add (    atch.nodeID    );
        }     
        
        //create instruction tree only up tp CCA
        BranchingInstructionTree instructionTree = instructionGenerator.getBranchingInstructionTree(  nodesChosenForfarming,   true);
        
        logger.info("\nInstruction tree \n" + instructionTree);
        
        
        /*
        //re-construct the migrated nodes with controlled branching
        logger.info("\nMerging leafs into new tree  \n"  );
        IloCplex cplexMerged= new IloCplex();   
        cplexMerged.importModel(MPS_FILE_ON_DISK);
        ActiveSubtree mergedAcriveSubTree =new ActiveSubtree(cplexMerged,  instructionTree, BILLION );
        mergedAcriveSubTree.solveTillLeafsMerged();
        
        exit(5);
        */
         
                
        //select 1 CCA, and continue solution
        
        FARMING_PHASE= false;
        
        IndexNode  selectedCCANode = ccaNodes.get(ZERO);
        logger.info("Selected CCA Node is  - " + selectedCCANode);
        //activeSubtree.pruneCCANode(selectedCCANode); comment out to ensure time taken without pruning is same as monolithic solve
        
        //construct new active subtree from cca node
        IloCplex cplexNew= new IloCplex();   
        cplexNew.importModel(MPS_FILE_ON_DISK);
        
        //cplexNew.exportModel("F:\\temporary files here\\logs\\testing\\msc98-ip.lp");
        ActiveSubtree activeSubtreeNew = new ActiveSubtree(cplexNew,instructionTree, activeSubtree.getSolution());
        // cplexNew.exportModel("F:\\temporary files here\\logs\\testing\\msc98-ip.node65.lp");
        activeSubtreeNew.solveTillLeafsMerged();       
        activeSubtreeNew.setDefaultCallbacks();
        
        //loop solve both sub trees
        logger.info("\nStarting full solve at " + LocalDateTime.now());
        double cutoff =  IS_MAXIMIZATION ? -BILLION:BILLION;
        while (! isHaltFilePresent()){
            
            cutoff = Math.min ( cutoff, Math.min(activeSubtreeNew.getSolution(), activeSubtree.getSolution()));
            logger.info("\nBEST Known solution is " +cutoff);
            
            logger.info("Initial activeSubtree best reamining obj val "+ activeSubtree.getBestObjValue() + " count of remaining leafs "+activeSubtree.getActiveLeafCount()
            + " branches "+ activeSubtree.getNumBranches()); 
            logger.info("New activeSubtree best reamining obj val "+ activeSubtreeNew.getBestObjValue()+ " count of remaining leafs "+activeSubtreeNew.getActiveLeafCount()
            + " branches "+ activeSubtreeNew.getNumBranches()); 
            
            long nodeCountOldtree = activeSubtree.getNumNodesSolved();
            long nodeCountNEwTree = activeSubtreeNew.getNumNodesSolved();
            
            activeSubtreeNew.setUpperCutoff(cutoff) ;
            activeSubtree .setUpperCutoff(cutoff) ;
           
            if (! activeSubtreeNew.isOptimal() ){
                logger.info("Solving new subtree ...");
                activeSubtreeNew.solveFor2Min ();
                logger.info("activeSubtreeNew status "+ activeSubtree.getStatus());
            } else  logger.info("Completed activeSubtree new" );
            
            
            
            
            if (! activeSubtreeNew.isFeasible() && activeSubtreeNew.getNumBranches()==ZERO){
                logger.info(" **** "+ activeSubtreeNew.getNumBranches());
                exit(3);
            }
            
            if (! activeSubtree.isOptimal() ) {
                 logger.info("Solving old subtree ...");
                activeSubtree.solveFor2Min ();
                logger.info("activeSubtree status "+ activeSubtree.getStatus());
            } else  logger.info("Completed activeSubtree");
            
            //must end completed active subtrees, to relase memory, see older implementations
            
            logger.info("Number of oldtree nodes solved is = "+  (activeSubtree.getNumNodesSolved()-nodeCountOldtree));
            logger.info("Number of newtree nodes solved is = "+  (activeSubtreeNew.getNumNodesSolved()-nodeCountNEwTree));
            
            if (  activeSubtreeNew.isOptimal()  && activeSubtree.isOptimal()) break;
            
            //temporaraily check
            if ((activeSubtree.getNumNodesSolved()-nodeCountOldtree)==ZERO && (activeSubtreeNew.getNumNodesSolved()-nodeCountNEwTree)==ZERO)  break;
             
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
