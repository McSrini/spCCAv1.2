/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_2.controlledBranching;

import static ca.mcmaster.spccav1_2.Constants.ZERO;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tamvadss
 * 
 * 
 * some metrics used by controlled branching. These are populated just before migration instructions are generated.
 */
public class CBMetrics {
        
    //descendant lists are populated just before generating farming instructions
    //These contain node IDs of descendants on left and right which have been chosen for farming
    public List <String> chosenLeafsThatAreLeftDescendants  = new ArrayList <String> ();
    public List <String> chosenLeafsThatAreRightDescendants = new ArrayList <String> ();
            
    //how many nodes on each side we can skip over
    //Note that we can have skippable nodes on both left and right sides of this node, however a skippable node can have skippable nodes on only 1 side
    //At each node, we can decide which direction to skip by checking the direction which has a non zero desecndant count
    public int leftSideSkipCount=ZERO, rightSideSkipCount= ZERO;
    
    public void clear() {
        chosenLeafsThatAreLeftDescendants.clear();
        chosenLeafsThatAreRightDescendants.clear();
        leftSideSkipCount=ZERO;
        rightSideSkipCount= ZERO;
    }
    
    public boolean isCleared(){
        return chosenLeafsThatAreLeftDescendants.size()==ZERO && chosenLeafsThatAreRightDescendants.size()==ZERO && leftSideSkipCount==ZERO && rightSideSkipCount== ZERO;
    }
    
}
