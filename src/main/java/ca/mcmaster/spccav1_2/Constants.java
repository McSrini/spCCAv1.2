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
public class Constants {
    
    
        
    public static final String EMPTY_STRING ="";
    public static final String MINUS_ONE_STRING = "-1";
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
    
    public static boolean BackTrack = false;
    
    //CCA represents this many nodes
    public static   int NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE  =  4; 
    
    //for testing, grow the tree this big
    public static final int TOTAL_LEAFS_IN_SOLUTION_TREE =  18 ;
    
    //CCA subtree allowed to have slightly less good leafs than expected 
    public static final double CCA_TOLERANCE_FRACTION =  0.1;
     
}
