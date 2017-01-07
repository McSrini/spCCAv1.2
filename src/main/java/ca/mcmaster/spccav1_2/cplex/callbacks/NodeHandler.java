/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_2.cplex.callbacks;
    
import static ca.mcmaster.spccav1_2.Constants.*;
import ca.mcmaster.spccav1_2.cca.IndexTree;
import ca.mcmaster.spccav1_2.cplex.datatypes.*;
import ilog.concert.IloException;
import ilog.cplex.IloCplex.NodeCallback;
import java.io.File;
import static java.lang.System.exit;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class NodeHandler extends NodeCallback {
    
    private static Logger logger=Logger.getLogger(NodeHandler.class);
     
    public long numNodesSolved = ZERO;
    public long activeLeafCount=ONE;
    
    public IndexTree indexTree;    
      
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+NodeHandler.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            exit(1);
        }
          
    }

    @Override
    protected void main() throws IloException {
        if(getNremainingNodes64()> ZERO){
            
            activeLeafCount= getNremainingNodes64();            
            numNodesSolved++;
              
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            NodeAttachment nodeData = (NodeAttachment) getNodeData(ZERO);
            if (nodeData==null ) { //it will be null for subtree root
               
                nodeData=new NodeAttachment (      );  
                setNodeData(ZERO,nodeData);                
                
            } 
            
                        
            if (FARMING_PHASE && getNremainingNodes64()>=TOTAL_LEAFS_IN_SOLUTION_TREE) {
                // this is where we set the stage for farming
                logger.info("Farming ...");
                indexTree=new IndexTree(getAllLeafs());
                
                //stop  
                abort();
            }
            
        }
          
    }
    
    private List<NodeAttachment> getAllLeafs () throws IloException {
        List<NodeAttachment> allLeafs = new ArrayList<NodeAttachment>();
        long numLeafs = getNremainingNodes64();
        for (int index = ZERO ; index < numLeafs; index ++){
            allLeafs.add((NodeAttachment)getNodeData(index) );
            logger.debug((NodeAttachment)getNodeData(index));
        }
        
        markMigratableNodes (  allLeafs);
        return allLeafs;
    }
    
     
    
    private void markMigratableNodes (List<NodeAttachment> allleafs) throws IloException{
        List<String> exclusionList = Arrays.asList("Node25", "Node23", "Node24","Node13", "Node29", "Node30");
        long numLeafs = getNremainingNodes64();
        
        for (int index = ZERO ; index < numLeafs; index ++){
            NodeAttachment node = (NodeAttachment)getNodeData(index) ;
            if ( exclusionList.contains(node.nodeID) ) node.isMigrateable = false;
            logger.info(node);
        }
    }
    
    
}
