/**
 * For license details see associated LICENSE.txt file.
 */

package uk.ac.ed.bio.synthsys.SBMLDataTools;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.AssignmentRule;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;


/**
 * Helper class used to add external data set into SBML models.
 * 
 * @author Ally Hume
 */
public class SBMLTimeCourseDataHelper {

    /**
     * Adds time series data (represented as a polynomial spline function) as a parameter to the 
     * given SBML model. 
     * <p>
     * The parameter is added to the SBML as a time-dependent assignment rule constructed using 
     * piecewise functions for each portion of the spline. The function will return a value only 
     * within the range of the spline function. 
     * 
     * @param sbmlModel      SBML model to which the parameter is to be added
     * @param parameterName  name of the parameter in the SBML document
     * @param psf            polynomial spline function
     */
    public static void addParameter(
            Model sbmlModel, String parameterName, PolynomialSplineFunction psf) {

        // Basic parameter checking. As this is an library best to give users errors that relate
        // to their usage rather than library internals.
        if (sbmlModel == null) 
            throw new IllegalArgumentException("sbmlModel parameter cannot be null");
        if (parameterName == null) 
            throw new IllegalArgumentException("parameterName parameter cannot be null");
        if (parameterName.length() == 0) 
            throw new IllegalArgumentException("parameterName parameter cannot be empty string");
        if (psf == null) throw new IllegalArgumentException("psf parameter cannot be null");
        
        PolynomialFunction[] polys = psf.getPolynomials();
        double[]             knots = psf.getKnots();
        
        Parameter parameter = sbmlModel.createParameter(parameterName);
        parameter.setName(parameterName);
        parameter.setConstant(false);
        
        AssignmentRule assignmentRule = sbmlModel.createAssignmentRule();
        
        ASTNode piecewise = new ASTNode(ASTNode.Type.FUNCTION_PIECEWISE);
        for (int i=0; i<polys.length; ++i) {
           ASTNode pfAST = createPolynomialFunctionAST(polys[i], knots[i]);
           ASTNode knotCondition = createKnotCondition(knots[i],knots[i+1]);
           piecewise.addChild(pfAST);
           piecewise.addChild(knotCondition);
        }

        assignmentRule.setMath(piecewise);
        assignmentRule.setVariable(parameter);
    }
    
    /**
     * Adds the given time course data as a parameter to the SBML model.  The data will be
     * represented in the model using a cubic spline.
     * <p>
     * The lengths of the time and values arrays must be equal and must be greater or equal to 3.
     * <p>
     * The time values must be sorted in ascending order.
     * <p>
     * If fittedTimes and fittedValues are specified then the value of the fitted curve will be
     * written to the fittedValues array for the time values in the associated fittedTimes array.
     * 
     * @param sbmlModel      SBML model to which the parameter is to be added
     * @param parameterName  name of the parameter to add
     * @param times          time values for time course data
     * @param values         data values for time course data
     * @param fittedTimes    time values for output fitted data, or null
     * @param fittedValues   array to which the output fitted data will be written, or null
     */
    public static void addParameterUsingCubicSpline(
            Model sbmlModel, String parameterName, 
            double[] times, double[] values,
            double[] fittedTimes, double[] fittedValues) {
        
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
        
        // Fit a cubic spline to the data
        SplineInterpolator si = new SplineInterpolator();
        PolynomialSplineFunction psf = si.interpolate(times, values);
            
        // Add the data to the model
        SBMLTimeCourseDataHelper.addParameter(sbmlModel, parameterName, psf);
        
        // Create the fitted data
        if (fittedTimes != null ) {
            for (int i=0; i<fittedTimes.length; ++i) {
                fittedValues[i] = psf.value(fittedTimes[i]);
            }
        }
    }
    
    /**
     * Adds the given time course data as a parameter to the SBML model.  The data will be
     * represented in the model using a cubic spline.
     * <p>
     * The lengths of the time and values arrays must be equal and must be greater or equal to 3.
     * <p>
     * The time values must be sorted in ascending order.
     * 
     * @param sbmlModel      SBML model to which the parameter is to be added
     * @param parameterName  name of the parameter to add
     * @param times          time values for time course data
     * @param values         data values for time course data
     */
    public static void addParameterUsingCubicSpline(
            Model sbmlModel, String parameterName, double[] times, double[] values) {
        
        addParameterUsingCubicSpline(sbmlModel, parameterName, times, values, null, null);
    }
    
    /**
     * Adds the given time course data as a parameter to the SBML model.  The data will be
     * represented in the model using a cubic spline.
     * <p>
     * The sizes of the time and values lists must be equal and must be greater than or equal to 3.
     * <p>
     * The time values must be sorted in ascending order.
     * 
     * @param sbmlModel      SBML model to which the parameter is to be added
     * @param parameterName  name of the parameter to add
     * @param times          time values for time course data
     * @param values         data values for time course data
     * @param fittedTimes    time values at which to compute the value of the fitted curve
     * @param fittedValues   empty list to which the fitted values are added
     */
    public static void addParameterUsingCubicSpline(
            Model sbmlModel, String parameterName, List<Double> times, List<Double> values,
            List<Double> fittedTimes, List<Double> fittedValues) {
        
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
        
        addParameterUsingCubicSpline(
                sbmlModel, parameterName, toPrimitiveArray(times), toPrimitiveArray(values),
                fittedTimesPrimitiveArray, fittedValuesPrimitiveArray);
        
        if (fittedTimes != null) {
            for (int i=0; i<fittedTimesPrimitiveArray.length; ++i) {
                fittedValues.add(new Double(fittedValuesPrimitiveArray[i]));
            }
        }
    }
    
    /**
     * Adds the given time course data as a parameter to the SBML model.  The data will be
     * represented in the model using a cubic spline.
     * <p>
     * The sizes of the time and values lists must be equal and must be greater than or equal to 3.
     * <p>
     * The time values must be sorted in ascending order.
     * 
     * @param sbmlModel      SBML model to which the parameter is to be added
     * @param parameterName  name of the parameter to add
     * @param times          time values for time course data
     * @param values         data values for time course data
     */
    public static void addParameterUsingCubicSpline(
            Model sbmlModel, String parameterName, List<Double> times, List<Double> values) {
        
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
                
        addParameterUsingCubicSpline(
                sbmlModel, parameterName, toPrimitiveArray(times), toPrimitiveArray(values));
    }
    
    /**
     * Creates the AST node that contains the logical condition that returns true when the model
     * time value lies in the range between knot0 and knot1 inclusive.
     * 
     * @param knot0  the lower boundary of the range 
     * @param knot1  the higher boundary of the range
     * 
     * @return AST node for the range condition
     */
    private static ASTNode createKnotCondition(double knot0, double knot1) {
        ASTNode and = new ASTNode(ASTNode.Type.LOGICAL_AND);
        ASTNode geq = new ASTNode(ASTNode.Type.RELATIONAL_GEQ);
        geq.addChild(new ASTNode(ASTNode.Type.NAME_TIME));
        geq.addChild(new ASTNode(knot0));
        ASTNode leq = new ASTNode(ASTNode.Type.RELATIONAL_LEQ);
        leq.addChild(new ASTNode(ASTNode.Type.NAME_TIME));
        leq.addChild(new ASTNode(knot1));
        and.addChild(geq);
        and.addChild(leq);
        return and;
    }
    
    /**
     * Creates the polynomial function AST that encodes the give polynomial function.
     * 
     * @param pf     polynomial function. The starting knot corresponds to zero for this function.
     * @param knot   starting knot for this function
     * 
     * @return AST node representing the function
     */
    private static ASTNode createPolynomialFunctionAST(PolynomialFunction pf, double knot) {
        double[] coefficients = pf.getCoefficients();
        
        ASTNode sum = new ASTNode(ASTNode.Type.PLUS);
        
        // Coefficients start with the constant then x^1, x^2 and so on
        sum.addChild(new ASTNode(coefficients[0]));
        for (int i=1; i<coefficients.length; ++i) {
            sum.addChild(
                    ASTNode.times(
                            new ASTNode(coefficients[i]),
                            ASTNode.pow(createTimeMinusOffsetAST(knot), i)));
        }
        return sum;
    }
    
    /**
     * Create an AST node that represents the model time with the given offset subtracted.
     * 
     * @param offset offset to be subtracted from the model time
     * 
     * @return AST node
     */
    private static ASTNode createTimeMinusOffsetAST(double offset) {
        return ASTNode.diff(new ASTNode(ASTNode.Type.NAME_TIME), new ASTNode(offset));
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
     * values must be identical, there most be at least three data points and the times must be
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
