/**
 * For license details see associated LICENSE.txt file.
 */

package uk.ac.ed.bio.SynthSys.SBMLDataTools;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.AssignmentRule;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Rule;


/**
 * Helper class used to add external data set into SBML models.
 * 
 * @author Ally Hume
 */
public class SBMLTimeCourseDataHelper {
    
    /**
     * Adds the given time course data as a parameter to the SBML model using the given 
     * interpolation type.
     * <p>
     * The lengths of the time and values arrays must be equal.
     * <p>
     * The time values must be sorted in ascending order.
     * <p>
     * If fittedTimes and fittedValues are specified then the value of the fitted curve will be
     * written to the fittedValues array for the time values in the associated fittedTimes array.
     * 
     * @param sbmlModel          SBML model to which the parameter is to be added
     * @param parameterName      name of the parameter to add
     * @param times              time values for time course data
     * @param values             data values for time course data
     * @param fittedTimes        time values for output fitted data, or null
     * @param fittedValues       array to which the output fitted data will be written, or null
     * @param interpolator       interpolation strategy to use
     */
    public static void addParameter(
            Model sbmlModel, String parameterName, 
            double[] times, double[] values,
            double[] fittedTimes, double[] fittedValues,
            Interpolator interpolator) {
        
        // Basic parameter checking. As this is an library best to give users errors that relate
        // to their usage rather than library internals.
        if (sbmlModel == null) 
            throw new IllegalArgumentException("sbmlModel parameter cannot be null");
        if (parameterName == null) 
            throw new IllegalArgumentException("parameterName parameter cannot be null");
        if (parameterName.length() == 0) 
            throw new IllegalArgumentException("parameterName parameter cannot be empty string");
        if (times == null) 
            throw new IllegalArgumentException("times parameter cannot be null");
        if (values == null) 
            throw new IllegalArgumentException("values parameter cannot be null");
        if (fittedTimes == null && fittedValues != null) {
            throw new IllegalArgumentException(
                    "fittedValues parameter cannot be null if fittedTimes parameter is not null");
        }
        if (fittedTimes != null && fittedValues == null) {
            throw new IllegalArgumentException(
                    "fittedTimes parameter cannot be null if fittedValues parameter is not null");
        }
        if (fittedTimes != null && fittedTimes.length != fittedValues.length) {
            throw new IllegalArgumentException(
                    "fittedTimes and fittedValues parameters must be arrays of equal length");
        }
        
        validateData(times, values);
        interpolator.setData(times,  values);
        
        // If the parameter name exists the remove it
        if (sbmlModel.containsParameter(parameterName)) {
            System.out.println("WARNING: contains parameter " + parameterName + ". Removing it.");
            sbmlModel.removeParameter(parameterName);
        }
        
        // If an assignment rule exists for the parameter then remove it
        List<Rule> rulesToDelete = new LinkedList<Rule>();
        for (Rule rule : sbmlModel.getListOfRules()) {
            if (rule.isAssignment()) {
                AssignmentRule assignmentRule = (AssignmentRule) rule;
                if (assignmentRule.getVariable().equals(parameterName)){
                    rulesToDelete.add(assignmentRule);
                }
            }
        }
        for (Rule rule : rulesToDelete) {
            sbmlModel.removeRule(rule);
        }
                
        Parameter parameter = sbmlModel.createParameter(parameterName);
        parameter.setName(parameterName);
        parameter.setConstant(false);
        
        AssignmentRule assignmentRule = sbmlModel.createAssignmentRule();
        
        List<ASTNode> functions          = interpolator.getFunctions();
        List<ASTNode> functionConditions = interpolator.getFunctionConditions();
        
        ASTNode piecewise = new ASTNode(ASTNode.Type.FUNCTION_PIECEWISE);
        for (int i=0; i<functions.size(); ++i) {
           piecewise.addChild(functions.get(i));
           piecewise.addChild(functionConditions.get(i));
        }
        assignmentRule.setMath(piecewise);
        assignmentRule.setVariable(parameter);
        
        // Create the fitted data
        if (fittedTimes != null ) {
            for (int i=0; i<fittedTimes.length; ++i) {
                fittedValues[i] = interpolator.value(fittedTimes[i]);
            }
        }
    }
    
    /**
     * Adds the given time course data as a parameter to the SBML model.
     * <p>
     * The lengths of the time and values arrays must be equal.
     * <p>
     * The time values must be sorted in ascending order.
     * 
     * @param sbmlModel          SBML model to which the parameter is to be added
     * @param parameterName      name of the parameter to add
     * @param times              time values for time course data
     * @param values             data values for time course data
     * @param interpolator       interpolation strategy to use
     */
    public static void addParameter(
            Model sbmlModel, String parameterName, double[] times, double[] values,
            Interpolator interpolator) {
        
        addParameter(sbmlModel, parameterName, times, values, null, null, interpolator);
    }
    
    /**
     * Adds the given time course data as a parameter to the SBML model.
     * <p>
     * The sizes of the time and values lists must be equal.
     * <p>
     * The time values must be sorted in ascending order.
     * 
     * @param sbmlModel          SBML model to which the parameter is to be added
     * @param parameterName      name of the parameter to add
     * @param times              time values for time course data
     * @param values             data values for time course data
     * @param fittedTimes        time values at which to compute the value of the fitted curve
     * @param fittedValues       empty list to which the fitted values are added
     * @param interpolator       interpolation strategy to use
     */
    public static void addParameter(
            Model sbmlModel, String parameterName, List<Double> times, List<Double> values,
            List<Double> fittedTimes, List<Double> fittedValues,
            Interpolator interpolator) {
        
        // Basic parameter checking. As this is an library best to give users errors that relate
        // to their usage rather than library internals.
        if (sbmlModel == null) 
            throw new IllegalArgumentException("sbmlModel parameter cannot be null");
        if (parameterName == null) 
            throw new IllegalArgumentException("parameterName parameter cannot be null");
        if (parameterName.length() == 0) 
            throw new IllegalArgumentException("parameterName parameter cannot be empty string");
        if (times == null) 
            throw new IllegalArgumentException("times parameter cannot be null");
        if (values == null) 
            throw new IllegalArgumentException("values parameter cannot be null");
        if (fittedTimes == null && fittedValues != null) {
            throw new IllegalArgumentException(
                    "fittedValues parameter cannot be null if fittedTimes parameter is not null");
        }
        if (fittedTimes != null && fittedValues == null) {
            throw new IllegalArgumentException(
                    "fittedTimes parameter cannot be null if fittedValues parameter is not null");
        }
        if (fittedValues != null && fittedValues.size() != 0) {
            throw new IllegalArgumentException(
                    "fittedValues parameter must be an empty list");
        }
        
        double[] fittedTimesPrimitiveArray = null;
        double[] fittedValuesPrimitiveArray = null;

        if (fittedTimes != null) {
            fittedTimesPrimitiveArray = toPrimitiveArray(fittedTimes);
            fittedValuesPrimitiveArray = new double[fittedTimesPrimitiveArray.length];
        }
        
        addParameter(
                sbmlModel, parameterName, toPrimitiveArray(times), toPrimitiveArray(values),
                fittedTimesPrimitiveArray, fittedValuesPrimitiveArray, interpolator);
        
        if (fittedTimes != null) {
            for (int i=0; i<fittedTimesPrimitiveArray.length; ++i) {
                fittedValues.add(new Double(fittedValuesPrimitiveArray[i]));
            }
        }
    }
    
    /**
     * Adds the given time course data as a parameter to the SBML model.
     * <p>
     * The sizes of the time and values lists must be equal.
     * <p>
     * The time values must be sorted in ascending order.
     * 
     * @param sbmlModel          SBML model to which the parameter is to be added
     * @param parameterName      name of the parameter to add
     * @param times              time values for time course data
     * @param values             data values for time course data
     * @param interpolator       interpolation strategy to use
     */
    public static void addParameter(
            Model sbmlModel, String parameterName, List<Double> times, List<Double> values,
            Interpolator interpolator) {
        
        // Basic parameter checking. As this is an library best to give users errors that relate
        // to their usage rather than library internals.
        if (sbmlModel == null) 
            throw new IllegalArgumentException("sbmlModel parameter cannot be null");
        if (parameterName == null) 
            throw new IllegalArgumentException("parameterName parameter cannot be null");
        if (parameterName.length() == 0) 
            throw new IllegalArgumentException("parameterName parameter cannot be empty string");
        if (times == null) 
            throw new IllegalArgumentException("times parameter cannot be null");
        if (values == null) 
            throw new IllegalArgumentException("values parameter cannot be null");
                
        addParameter(
                sbmlModel, parameterName, toPrimitiveArray(times), toPrimitiveArray(values),
                interpolator);
    }
    
    /**
     * Converts a list to Double objects to an array of double primitives.
     * 
     * @param list  list to convert
     * 
     * @return newly created array of double primitives
     */
    private static double[] toPrimitiveArray(List<Double> list) {
        Double[] array = list.toArray(new Double[list.size()]);
        return ArrayUtils.toPrimitive(array);
    }
    
    /**
     * Validates the data set to ensure it is valid for spline fitting.  The number of times and
     * values must be identical, there must be at least three data points and the times must be
     * in ascending order.
     * 
     * @param times    time point
     * @param values   data values at the time points
     * 
     * @throws IllegalArgumentException if the data is not valid
     */
    private static void validateData(double[] times, double[] values) {
        if (times.length != values.length) throw new IllegalArgumentException(
                "Number of data points in values parameter differs from times parameter");
        
        if (times.length < 3) throw new IllegalArgumentException(
                "Data in the times and values parameters must contain at least 3 data points");
        
        // Check the times data is ascending
        for (int i=0; i<times.length-1; ++i) {
            if (times[i+1] <= times[i]) throw new IllegalArgumentException(
                    "Data in times parameter must be in ascending order");
        }
    }
}
