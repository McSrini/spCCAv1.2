/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_2;

import ca.mcmaster.spccav1_2.cca.IndexNode;
import ca.mcmaster.spccav1_2.cplex.ActiveSubtree;
import ca.mcmaster.spccav1_2.cplex.datatypes.BranchingInstruction;
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
public class Driver {
    
    private static  Logger logger = null;
        
    public static final String EMPTY_STRING ="";
    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final int TWO = 2;    
    public static final int TEN = 10;  
    public static final int SIXTY = 60;  
    public static final int BILLION = 1000000000;
    
    public static final String MPS_FILE_ON_DISK =  "F:\\temporary files here\\a1c1s1.mps";
    public static final String LOG_FOLDER="F:\\temporary files here\\logs\\testing\\";
    public static final String LOG_FILE_EXTENSION = ".log";
    
    public static boolean FARMING_PHASE= true;
    public static final boolean IS_MAXIMIZATION = false;
    
    //CCA represents this many nodes
    public static   int NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE  =  4; 
    
    //for testing, grow the tree this big
    public static final int TOTAL_LEAFS_IN_SOLUTION_TREE =  18 ;
    
    //CCA subtree allowed to have slightly less good leafs than expected 
    public static final double CCA_TOLERANCE_FRACTION =  0.1;
        
    public static void main(String[] args) throws Exception {
     
        logger=Logger.getLogger(Driver.class);
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+Driver.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            exit(1);
        }
        
        IloCplex cplex= new IloCplex();   
        cplex.importModel(MPS_FILE_ON_DISK);
        ActiveSubtree activeSubtree = new ActiveSubtree(cplex, null, null) ;
        
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
         
        activeSubtree.solve();
        List<IndexNode> ccaNodes = activeSubtree.getCCANodes();
          
        for (IndexNode node: ccaNodes){
            logger.info(node);
        }
        
        
        /*
        IndexNode testNode = activeSubtree.getCCANode( Arrays.asList("Node14", "Node33" )) ;
        logger.info(testNode);
        exit(5);
        */
                
        //select 1 CCA, and continue solution
        
        FARMING_PHASE= false;
        
        IndexNode  selectedCCANode = ccaNodes.get(ONE);
        logger.info("Selected CCA Node is  - " + selectedCCANode);
        activeSubtree.selectCCANode(selectedCCANode);
        
        //construct new active subtree from cca node
        IloCplex cplexNew= new IloCplex();   
        cplexNew.importModel(MPS_FILE_ON_DISK);
        //cplexNew.exportModel("F:\\temporary files here\\logs\\testing\\msc98-ip.lp");
        ActiveSubtree activeSubtreeNew = new ActiveSubtree(cplexNew, getLowerBounds(selectedCCANode.cumulativeBranchingInstructions) , 
                getUpperBounds(selectedCCANode.cumulativeBranchingInstructions)  ) ;
        // cplexNew.exportModel("F:\\temporary files here\\logs\\testing\\msc98-ip.node65.lp");
        
        //loop solve both sub trees
        logger.info("Starting full solve at " + LocalDateTime.now());
        double cutoff =  BILLION;
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

    private static boolean isHaltFilePresent (){
        File file = new File("F:\\temporary files here\\haltfile.txt");
         
        return file.exists();
    }
    
}
