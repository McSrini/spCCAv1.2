/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_2;

import static ca.mcmaster.spccav1_2.Constants.*;
import ca.mcmaster.spccav1_2.cplex.ActiveSubtree;
import ilog.cplex.IloCplex;
import java.io.File;
import static java.lang.System.exit;
import java.time.LocalDateTime;
import org.apache.log4j.*;
import org.apache.log4j.PatternLayout;

/**
 *
 * @author tamvadss
 */
public class TestSimple {
    
    private static  Logger logger = null;
    
    public static void main(String[] args) throws Exception {
        logger=Logger.getLogger(TestSimple.class);
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+TestSimple.class.getSimpleName()+ LOG_FILE_EXTENSION);
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
        
        logger.info("Started full solve at " + LocalDateTime.now()); 
        
        while (! isHaltFilePresent()){
            if (! activeSubtree.isOptimal()) activeSubtree.solveFor2Min (); else  logger.info("Completed activeSubtree");
            
            logger.info("  best reamining obj val "+ activeSubtree.getBestObjValue() +  " current best soln is " + activeSubtree.getSolution());
            
        }
        
        if (activeSubtree.isOptimal()) logger.info("M0 solution is "+activeSubtree.getSolution());
        logger.info("Completed full solve at " + LocalDateTime.now()); 
    
    }
        
    private static boolean isHaltFilePresent (){
        File file = new File("F:\\temporary files here\\haltfile.txt");
         
        return file.exists();
    }
    

}
