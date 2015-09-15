/**
 * For license details see associated LICENSE.txt file.
 */

package uk.ac.ed.bio.SynthSys.SBMLDataTools;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.sbml.jsbml.ASTNode;

/**
 * Interpolates data using piecewise polynomial functions. It can therefore be used for linear
 * interpolation as well a cubic spline interpolation.
 */
public class PolynomialInterpolator implements Interpolator {

    // Univariate interpolated wrapped by this class
    private final UnivariateInterpolator _univariateInterpolator;
    
    // Polynomial spline function produced by the interpolation. 
    private PolynomialSplineFunction _psf;  
    
    /**
     * Constructs a polynomial interpolator to wrap the given univariate interpolator.
     * 
     * @param univariantInterpolator univariant interpolator to wrap
     */
    public PolynomialInterpolator( UnivariateInterpolator univariantInterpolator ) {
        _univariateInterpolator = univariantInterpolator;
    }

    /**
     * {@inheritDoc}
     */
    public void setData(double[] times, double[] values) {
        _psf = (PolynomialSplineFunction) _univariateInterpolator.interpolate(times, values);
    }

    /**
     * {@inheritDoc}
     */
    public double value(double time) {
        return _psf.value(time);
    }

    /**
     * {@inheritDoc}
     */
    public List<ASTNode> getFunctions() {
        List<ASTNode> result = new LinkedList<ASTNode>();
        
        PolynomialFunction[] polys = _psf.getPolynomials();
        double[]             knots = _psf.getKnots();
        
        for (int i=0; i<polys.length; ++i) {
           result.add(createPolynomialFunctionAST(polys[i], knots[i]));
        }
        
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public List<ASTNode> getFunctionConditions() {
        List<ASTNode> result = new LinkedList<ASTNode>();
        
        PolynomialFunction[] polys = _psf.getPolynomials();
        double[]             knots = _psf.getKnots();
        
        for (int i=0; i<polys.length; ++i) {
            result.add(createKnotCondition(knots[i],knots[i+1]));
        }
        
        return result;
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

}
