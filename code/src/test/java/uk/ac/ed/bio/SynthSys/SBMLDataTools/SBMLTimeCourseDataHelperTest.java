/**
 * For license details see associated LICENSE.txt file.
 */

package uk.ac.ed.bio.synthsys.SBMLDataTools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.AssignmentRule;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Rule;
import org.sbml.jsbml.SBMLDocument;

import uk.ac.ed.bio.synthsys.SBMLDataTools.SBMLTimeCourseDataHelper;

/**
 * Test class for SBMLTimeCourseDataHelper
 * 
 * @author Ally Hume
 */
public class SBMLTimeCourseDataHelperTest {

    // Times and value data
    private double _times[];
    private double _values[];

    /**
     * Tests that the correct exception is produced when passing a NULL model.
     */
    @Test
    public void addParameterUsingCubicSplineArrayVersionNullModel() {

        boolean caughtException = false;
        setSinData();
        
        try {
            SBMLTimeCourseDataHelper.addParameterUsingCubicSpline(null, "myParam", _times, _values);
        }
        catch(IllegalArgumentException iae)
        {
            caughtException = true;
            assertEquals("sbmlModel parameter cannot be null", iae.getMessage());
        }
        
        if (!caughtException) fail("Expected IllegalArgumentException");
        
    }

    /**
     * Tests that the correct exception is produced when passing a NULL parameter name.
     */
    @Test
    public void addParameterUsingCubicSplineArrayVersionNullParameterName() {

        boolean caughtException = false;
        setSinData();
        SBMLDocument doc = new SBMLDocument(3, 1);
        Model model = doc.createModel("test_model");
        
        try {
            SBMLTimeCourseDataHelper.addParameterUsingCubicSpline(model, null, _times, _values);
        }
        catch(IllegalArgumentException iae)
        {
            caughtException = true;
            assertEquals("parameterName parameter cannot be null", iae.getMessage());
        }
        
        if (!caughtException) fail("Expected IllegalArgumentException");
    }

    /**
     * Tests that the correct exception is produced when passing NULL times.
     */
    @Test
    public void addParameterUsingCubicSplineArrayVersionNullTimes() {

        boolean caughtException = false;
        setSinData();
        SBMLDocument doc = new SBMLDocument(3, 1);
        Model model = doc.createModel("test_model");
        
        try {
            SBMLTimeCourseDataHelper.addParameterUsingCubicSpline(model, "myParam", null, _values);
        }
        catch(IllegalArgumentException iae)
        {
            caughtException = true;
            assertEquals("times parameter cannot be null", iae.getMessage());
        }
        
        if (!caughtException) fail("Expected IllegalArgumentException");
    }

    /**
     * Tests that the correct exception is produced when passing NULL values.
     */
    @Test
    public void addParameterUsingCubicSplineArrayVersionNullValues() {

        boolean caughtException = false;
        setSinData();
        SBMLDocument doc = new SBMLDocument(3, 1);
        Model model = doc.createModel("test_model");
        
        try {
            SBMLTimeCourseDataHelper.addParameterUsingCubicSpline(model, "myParam", _times, null);
        }
        catch(IllegalArgumentException iae)
        {
            caughtException = true;
            assertEquals("values parameter cannot be null", iae.getMessage());
        }
        
        if (!caughtException) fail("Expected IllegalArgumentException");
    }
    
    /**
     * Tests that the correct exception is produced when passing times and values arrays of 
     * different lengths.
     */
    @Test
    public void addParameterUsingCubicSplineArrayVersionDifferentLengthArrays() {
        boolean caughtException = false;
        setSinData();
        SBMLDocument doc = new SBMLDocument(3, 1);
        Model model = doc.createModel("test_model");
        
        // Make longer values array
        double[] values = new double[_values.length+1];
        System.arraycopy(_values, 0, values, 0, _values.length);
        values[values.length-1] = 0.0;
        
        try {
            SBMLTimeCourseDataHelper.addParameterUsingCubicSpline(model, "myParam", _times, values);
        }
        catch(IllegalArgumentException iae)
        {
            caughtException = true;
            assertEquals(
                    "Number of data points in values parameter differs from times parameter", 
                    iae.getMessage());
        }
        
        if (!caughtException) fail("Expected IllegalArgumentException");        
    }
    
    /**
     * Tests that the correct exception is produced when passing too few data points.
     */
    @Test
    public void tooFewDataPoints() {
        boolean caughtException = false;
        
        _times = new double[]{ -3.0, -2.0 };
        _values = new double[_times.length];
        
        for (int i=0; i<_times.length; ++i) {
            _values[i] = Math.sin(_times[i]);
        }

        SBMLDocument doc = new SBMLDocument(3, 1);
        Model model = doc.createModel("test_model");
        
        try {
            SBMLTimeCourseDataHelper.addParameterUsingCubicSpline(model, "myParam", _times, _values);
        }
        catch(IllegalArgumentException iae) {
            caughtException = true;
            assertEquals(
                    "Data in the times and values parameters must contain at least 3 data points",
                    iae.getMessage());
        }
        if (!caughtException) fail("Expected IllegalArgumentException");        
    }
    
    /**
     * Tests that the correct exception is produced when passing times data that is not in 
     * ascending order.
     */
    @Test
    public void timesDataNotAscending() {
        boolean caughtException = false;
        
        _times = new double[]{ -3.0, -2.0, -1.0, 0, -1.0};
        _values = new double[_times.length];
        
        for (int i=0; i<_times.length; ++i) {
            _values[i] = Math.sin(_times[i]);
        }

        SBMLDocument doc = new SBMLDocument(3, 1);
        Model model = doc.createModel("test_model");
        
        try {
            SBMLTimeCourseDataHelper.addParameterUsingCubicSpline(model, "myParam", _times, _values);
        }
        catch(IllegalArgumentException iae) {
            caughtException = true;
            assertEquals(
                    "Data in times parameter must be in ascending order",
                    iae.getMessage());
        }
        if (!caughtException) fail("Expected IllegalArgumentException");   
    }
    
    /**
     * Tests that correct execution produced fitted data that closely matches the function that
     * was sampled to produce the external data.  Here the sine function is used.
     */
    @Test
    public void normalExcecution() {
        setSinData();
        SBMLDocument doc = new SBMLDocument(3, 1);
        Model model = doc.createModel("test_model");

        SBMLTimeCourseDataHelper.addParameterUsingCubicSpline(model, "myParam", _times, _values);
        
        // Model must now have a parameter called myParam with specific properties
        assertEquals("Model must have one parameter", 1, model.getParameterCount());
        Parameter param = model.getParameter(0);
        assertEquals("Parameter id: ",         "myParam", param.getId());
        assertEquals("Parameter name: ",       "myParam", param.getName());
        assertEquals("Parameter isConstant: ", false,     param.isConstant());

        // Model must have an assignment rule that is associated with the parameter
        assertEquals("Model must have one rule", 1, model.getRuleCount());
        Rule rule = model.getRule(0);
        assertTrue("Rule must be assignment rule", rule instanceof AssignmentRule);
        AssignmentRule assignmentRule = (AssignmentRule) rule;
        assertEquals("Variable of assignment rule", "myParam", assignmentRule.getVariable());
        
        // Now we can compare data with the fitted data - for the sine function the spline
        // should fit quite well
        for (double t = _times[0]; t<_times[_times.length-1]; t+=0.005) {
            double sin = Math.sin(t);
            double fitted = evaluateMathML(assignmentRule.getMath(), t);
            
            assertTrue(
                    "Fitted must be close to actual, t=" + t + " sine="+sin +" fitted=" + fitted + 
                        " diff=" + Math.abs(sin-fitted), 
                    Math.abs(sin-fitted) < 0.01);
        }
    }

    
    /**
     * Helper method to evaluate a MathML equation at a given time.
     * 
     * @param node   MathML equation
     * @param t      time, the only parameter to the equation
     * 
     * @return the value of the equation at the specified time
     */
    private double evaluateMathML(ASTNode node, double t) {
        
        double result;
        
        switch(node.getType()) {
        
        case FUNCTION_PIECEWISE:
            for (int i=0; i<node.getNumChildren()/2; ++i ) {
                
                if (evaluateBooleanMathML(node.getChild(i*2+1), t)) {
                    return evaluateMathML(node.getChild(i*2), t);
                }
            }
            // error no match found
            throw new RuntimeException("No match found in the ranges of the piecewise");
            
        case POWER:
            result = Math.pow(evaluateMathML(node.getLeftChild(), t),
                            evaluateMathML(node.getRightChild(), t));
            return result;
        
        case TIMES:
            result = 1;
            for (int i=0; i<node.getNumChildren(); ++i) {
                result *= evaluateMathML(node.getChild(i), t); 
            }
            return result;
            
        case PLUS:
            result = 0;
            for (int i=0; i<node.getNumChildren(); ++i) {
                result += evaluateMathML(node.getChild(i), t); 
            }
            return result;
            
        case MINUS:
            result = evaluateMathML(node.getChild(0), t);
            for (int i=1; i<node.getNumChildren(); ++i) {
                result -= evaluateMathML(node.getChild(i), t); 
            }
            return result;

        case NAME_TIME:
            result = t;
            return result;
            
        case REAL:
        case INTEGER:
            result = Double.parseDouble(node.toString());
            return result;
            
        default:
            // Should probably throw an exception
            throw new RuntimeException("No rule for type: " + node.getType());
        }
    }
 
    /**
     * Evaluates a boolean Math ML expression
     * 
     * @param node  MathML node that is a boolean expression
     * @param t     time, the only parameter used in the expression
     * 
     * @return the boolean result of evaluating the expression
     */
    private boolean evaluateBooleanMathML(ASTNode node, double t) {
        
        boolean result = false;
        
        switch(node.getType()) {
        
        case LOGICAL_AND:
            result = evaluateBooleanMathML(
                    node.getLeftChild(), t) && evaluateBooleanMathML(node.getRightChild(), t);
            return result;
            
        case RELATIONAL_GEQ:
            result = evaluateMathML(
                    node.getLeftChild(), t) >= evaluateMathML(node.getRightChild(), t);
            return result;
            
        case RELATIONAL_LEQ:
            result = evaluateMathML(
                    node.getLeftChild(), t) <= evaluateMathML(node.getRightChild(), t);
            return result;
            
        default:
            throw new RuntimeException(
                    "No code to handle boolean expression of type " + node.getType());
        }
    }

    /**
     * Sets the _times and _values class variables to arrays that contains the time points and
     * values for a sine function.
     */
    private void setSinData() {
        _times = new double[]{ -3.0, -2.0, -1.0, 0, 1.0, 2.0, 3.0 };
        _values = new double[_times.length];
        
        for (int i=0; i<_times.length; ++i) {
            _values[i] = Math.sin(_times[i]);
        }
    }
}
