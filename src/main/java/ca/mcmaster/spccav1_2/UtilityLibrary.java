/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_2;
 
import static ca.mcmaster.spccav1_2.Driver.*;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class UtilityLibrary {
    
    
    private static Logger logger=Logger.getLogger(UtilityLibrary.class);
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+UtilityLibrary.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            exit(1);
        }
          
    }
    
    /**
     * 
     * To the CPLEX object ,  apply all the bounds mentioned in attachment
     */
    public static void  merge ( IloCplex cplex, Map< String,Double > lowerBounds, Map< String,Double > upperBounds  ) throws IloException {

        IloLPMatrix lpMatrix = (IloLPMatrix) cplex .LPMatrixIterator().next();

        //WARNING : we assume that every variable appears in at least 1 constraint or variable bound
        IloNumVar[] variables = lpMatrix.getNumVars();

        for (int index = ZERO ; index <variables.length; index ++ ){

            IloNumVar thisVar = variables[index];
            updateVariableBounds(thisVar,lowerBounds, false );
            updateVariableBounds(thisVar,upperBounds, true );

        }       
    }
    
    /**
     * 
     *  Update variable bounds as specified    
     */
    public static   void updateVariableBounds(IloNumVar var, Map< String,Double > newBounds, boolean isUpperBound   ) 
            throws IloException{

        String varName = var.getName();
        boolean isPresentInNewBounds = newBounds.containsKey(varName);

        if (isPresentInNewBounds) {
            double newBound =   newBounds.get(varName)  ;
            if (isUpperBound){
                if ( var.getUB() > newBound ){
                    //update the more restrictive upper bound
                    var.setUB( newBound );
                    logger.info(" var " + varName + " set bound " + newBound ) ;
                }
            }else{
                if ( var.getLB() < newBound){
                    //update the more restrictive lower bound
                    var.setLB(newBound);
                    logger.info(" var " + varName + " set bound " + newBound ) ;
                }
            }               
        }

    }  
    
}
