/**
 * For license details see associated LICENSE.txt file.
 */

package uk.ac.ed.bio.SynthSys.SBMLDataTools;

import java.util.List;

import org.sbml.jsbml.ASTNode;

/*
 * Interface for objects that can interpolate data. The object must be able to accept data to
 * be interpolated, return the interpolated value for a given input and also return the 
 * interpolation as piecewise functions in SBML format.
 */
public interface Interpolator {
    
    /**
     * Sets the data to be interpolated.
     * 
     * @param times    time points
     * @param values   values associated with the time points
     */
    void setData(double[] times, double[] values);
    
    /**
     * Gets the interpolated value at the given time point.
     * 
     * @param time  time point
     * 
     * @return interpolated value
     */
    double value(double time);
    
    /**
     * Gets a list of functions (in SBML notation) that define the interpolation.
     * 
     * @return list of ASTNodes defining the functions.
     */
    List<ASTNode> getFunctions();
    
    /**
     * Gets the list of conditions (in SBML notation) that define which each of the functions is
     * to be applied.
     * 
     * @return list of ASTNodes defining the conditions.  This list will be the same size of that
     *         returned by getFunctions().  Each condition in this list is associated with the
     *         corresponding function returned by getFunctions().
     */
    List<ASTNode> getFunctionConditions();
}
