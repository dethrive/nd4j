package org.nd4j.autodiff.samediff.impl;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Data;
import org.nd4j.autodiff.ArrayField;
import org.nd4j.autodiff.functions.DifferentialFunction;
import org.nd4j.autodiff.functions.Variable;
import org.nd4j.autodiff.opstate.NDArrayInformation;
import org.nd4j.autodiff.opstate.OpState;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.util.ArrayUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *
 * A variable representing a component within a
 * {@@link SameDiff} graph.
 *
 * SDVariable is used for symbolic declaration
 * of equations.
 *
 * @author Adam Gibson
 *
 */
@Data
public class SDVariable  implements Serializable {
    private INDArray arr;
    private Variable arrayField;
    private String varName;
    private SameDiff sameDiff;
    private int[] shape;
    private SDVariable gradient;
    private int vertexId;
    protected DifferentialFunction differentialFunction;

    @Builder
    private SDVariable(DifferentialFunction differentialFunction,
                       String varName,
                       INDArray arr,
                       SameDiff sameDiff,
                       Variable arrayField,
                       int[] shape,
                       int vertexId) {
        this.shape = shape;
        this.differentialFunction = differentialFunction;
        this.varName = varName;
        this.arr = arr;
        this.vertexId = vertexId;
        this.arrayField = arrayField;
        this.sameDiff = sameDiff;
        if(differentialFunction != null)
            this.vertexId = differentialFunction.getVertexId();
        else if(arrayField != null)
            this.vertexId = arrayField.getVertexId();

    }


    /**
     * Nicer looking alias
     * for the gradient variable.
     * The gradient variable is meant to be an
     * a variable representation
     * of the gradient represented
     * in the underlying {@link DifferentialFunction}
     * @return
     */
    public SDVariable gradient() {
        return getGradient();
    }

    /**
     * A getter for the variable gradient.
     * Note here that a lazy initialization of the
     * gradient variable will happen if the gradient
     * isn't present at this variable's initialization
     * but is set later.
     * @return
     */
    public SDVariable getGradient() {
        if(gradient == null && differentialFunction != null && differentialFunction.getGradient() != null) {
            this.gradient = differentialFunction != null && differentialFunction.getGradient() != null ? SDVariable.builder()
                    .sameDiff(sameDiff)
                    .differentialFunction(differentialFunction.getGradient())
                    .varName(varName + "-grad")
                    .arr(sameDiff.getNDArray(differentialFunction.getGradient().getOpState().getResult()))
                    .shape(differentialFunction.getGradient() != null ? differentialFunction.getGradient().getResultShape() : null)
                    .build() : null;
        }

        else if(gradient == null && arrayField != null && arrayField.getGradient() != null) {
            this.gradient = arrayField != null && arrayField.getGradient() != null ? SDVariable.builder()
                    .sameDiff(sameDiff)
                    .differentialFunction(arrayField.getGradient())
                    .varName(varName + "-grad").arr(sameDiff.getNDArray(arrayField.getGradient().getOpState().getResult()))
                    .shape(arrayField.getGradient() != null ? arrayField.getGradient().getResultShape() : null)
                    .build() : null;
        }



        return gradient;
    }

    public void setGradient(SDVariable gradient) {
        this.gradient = gradient;
    }

    /**
     *
     * @return
     */
    public NDArrayInformation getInfo() {
        if(getArrayField() == null)
            return null;
        return getArrayField().getM_x().getInput();
    }

    /**
     *
     * @return
     */
    public String getFormula() {
        List<Variable> ret = new ArrayList<>();
        if(arrayField != null)
            return arrayField.getFormula(ret);
        else {
            return this.differentialFunction.getFormula(ret);
        }
    }


    /**
     * Returns the shape of this variable
     * @return
     */
    public int[] getShape() {
        if(shape != null)
            return shape;
        if(differentialFunction == null)
            throw new IllegalStateException("Unable to infer shape. Function is null.");
        OpState opState =  differentialFunction.getOpState();
        if(opState == null) {
            return differentialFunction.getValue(true).getInput().getShape();
        }

        return opState.getResult().getShape();

    }


    /**
     *
     * @return
     */
    public boolean isAllocated() {
        return arr != null;
    }

    /**
     *
     */
    public void allocate() {
        if(arr == null)
            arr = Nd4j.zeros(getShape());
    }


    /**
     *
     * @return
     */
    public SDVariable dup() {
        return SDVariable.builder()
                .differentialFunction(differentialFunction)
                .arrayField(arrayField)
                .varName(varName)
                .shape(shape)
                .sameDiff(sameDiff)
                .arr(arr != null ? arr.dup() : null)
                .build();
    }

    private int[] getTransformOutputShape(SDVariable other) {
        if(shape == null)
            return other.getShape();
        if(ArrayUtil.prod(shape) == 1) {
            return other.getShape();
        }

        return getShape();
    }





    //scalars

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable rsub(double sameDiffVariable) {
        return rsub("rsub(" + varName + sameDiffVariable + ")",sameDiffVariable);
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable rdiv(double sameDiffVariable) {
        return rdiv("rdiv(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable add(double sameDiffVariable) {
        return add("add(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable sub(double sameDiffVariable) {
        return sub("sub(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable div(double sameDiffVariable) {
        return div("div(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable mul(double sameDiffVariable) {
        return mul("mul(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }


    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable rsubi(double sameDiffVariable) {
        return rsubi("rsubi(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable rdivi(double sameDiffVariable) {
        return rdivi("rdivi(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable addi(double sameDiffVariable) {
        return addi("addi(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable subi(double sameDiffVariable) {
        return subi("subi(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable divi(double sameDiffVariable) {
        return divi("divi(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable muli(double sameDiffVariable) {
        return muli("muli(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }



    //end scalars


    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable rsub(SDVariable sameDiffVariable) {
        return rsub("rsub(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable rdiv(SDVariable sameDiffVariable) {
        return rdiv("rdiv(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable add(SDVariable sameDiffVariable) {
        return add("ad(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable sub(SDVariable sameDiffVariable) {
        return sub("sub(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable div(SDVariable sameDiffVariable) {
        return div("div(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable mul(SDVariable sameDiffVariable) {
        return mul("mul(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }


    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable rsubi(SDVariable sameDiffVariable) {
        return rsubi("rsubi(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable rdivi(SDVariable sameDiffVariable) {
        return rdivi("rdivi(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable addi(SDVariable sameDiffVariable) {
        return addi("addi(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable subi(SDVariable sameDiffVariable) {
        return subi("subi(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable divi(SDVariable sameDiffVariable) {
        return divi("divi(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable muli(SDVariable sameDiffVariable) {
        return muli("muli(" + varName + sameDiffVariable + ")",sameDiffVariable);

    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable rsub(String varName, double sameDiffVariable) {
        DifferentialFunction function = sameDiff.f().rsub(getFunction(this),sameDiffVariable);

        SDVariable ret = SDVariable.builder()
                .varName(varName )
                .arr(null).sameDiff(getSameDiff())
                .shape(getTransformOutputShape(this))
                .differentialFunction(function)
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable rdiv(String varName, double sameDiffVariable) {
        DifferentialFunction function = sameDiff.f().rdiv(getFunction(this),sameDiffVariable);

        SDVariable ret =  SDVariable.builder()
                .varName(varName )
                .arr(null).sameDiff(getSameDiff())
                .shape(getTransformOutputShape(this))
                .differentialFunction(function)
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable add(String varName, double sameDiffVariable) {
        DifferentialFunction function = sameDiff.f().add(getFunction(this),sameDiffVariable);
        SDVariable ret = SDVariable.builder()
                .varName(varName )
                .arr(null).sameDiff(getSameDiff())
                .shape(getTransformOutputShape(this))
                .differentialFunction(function)
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable sub(String varName, double sameDiffVariable) {
        DifferentialFunction right = getFunction(this);
        DifferentialFunction result = sameDiff.f().sub(right,sameDiffVariable);
        SDVariable ret =  SDVariable.builder()
                .varName(varName + " - " + "scalar")
                .arr(null).sameDiff(getSameDiff())
                .shape(getTransformOutputShape(this))
                .differentialFunction(result)
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable div(String varName, double sameDiffVariable) {
        DifferentialFunction function = sameDiff.f().div(getFunction(this),sameDiffVariable);

        SDVariable ret =  SDVariable.builder()
                .varName(varName + " / " + "scalar")
                .arr(null).sameDiff(getSameDiff())
                .shape(getTransformOutputShape(this))
                .differentialFunction(function)
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable mul(String varName, double sameDiffVariable) {
        DifferentialFunction function = sameDiff.f().mul(getFunction(this)
                ,sameDiffVariable);
        SDVariable ret = SDVariable.builder()
                .varName(varName + " * " + "scalar")
                .arr(null).sameDiff(getSameDiff())
                .shape(getTransformOutputShape(this))
                .differentialFunction(function)
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }


    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable rsubi(String varName, double sameDiffVariable) {
        DifferentialFunction function = sameDiff.f().rsubi(getFunction(this),sameDiffVariable);

        SDVariable ret =  SDVariable.builder()
                .varName(varName + " - " + "scalar")
                .arr(null).sameDiff(getSameDiff())
                .shape(getTransformOutputShape(this))
                .differentialFunction(function)
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable rdivi(String varName, double sameDiffVariable) {
        DifferentialFunction function = sameDiff.f().rdivi(getFunction(this)
                ,sameDiffVariable);

        SDVariable ret =  SDVariable.builder()
                .varName(varName + " / " + "scalar")
                .arr(null).sameDiff(getSameDiff())
                .shape(getTransformOutputShape(this))
                .differentialFunction(function)
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable addi(String varName, double sameDiffVariable) {
        DifferentialFunction function = sameDiff.f().addi(getFunction(this),sameDiffVariable);

        SDVariable ret = SDVariable.builder()
                .varName(varName )
                .arr(null).sameDiff(getSameDiff())
                .shape(getTransformOutputShape(this))
                .differentialFunction(function)
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable subi(String varName, double sameDiffVariable) {
        DifferentialFunction function = sameDiff.f().subi(getFunction(this),sameDiffVariable);

        SDVariable ret =  SDVariable.builder()
                .varName(varName + " - " + "scalar")
                .arr(null).sameDiff(getSameDiff())
                .shape(getTransformOutputShape(this))
                .differentialFunction(function)
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable divi(String varName, double sameDiffVariable) {
        DifferentialFunction function = sameDiff.f().divi(getFunction(this),sameDiffVariable);

        SDVariable ret =  SDVariable.builder()
                .varName(varName)
                .arr(null)
                .sameDiff(getSameDiff())
                .shape(getTransformOutputShape(this))
                .differentialFunction(function)
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable muli(String varName, double sameDiffVariable) {
        DifferentialFunction function = sameDiff.f().muli(getFunction(this),sameDiffVariable);

        SDVariable ret =  SDVariable.builder().sameDiff(getSameDiff())
                .varName(varName)
                .arr(null).sameDiff(getSameDiff())
                .shape(getTransformOutputShape(this))
                .differentialFunction(function)
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }



    //end scalars


    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable rsub(String varName, SDVariable sameDiffVariable) {
        assertShapeEquals(sameDiffVariable);

        SDVariable ret =  SDVariable.builder().sameDiff(getSameDiff())
                .varName(varName)
                .arr(null).sameDiff(sameDiffVariable.getSameDiff())
                .shape(getTransformOutputShape(sameDiffVariable))
                .differentialFunction(sameDiff.f().rsub(getFunction(this),getFunction(sameDiffVariable)))
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable rdiv(String varName, SDVariable sameDiffVariable) {
        assertShapeEquals(sameDiffVariable);

        SDVariable ret = SDVariable.builder().sameDiff(getSameDiff())
                .varName(varName)
                .arr(null).sameDiff(sameDiffVariable.getSameDiff()).shape(getTransformOutputShape(sameDiffVariable))
                .differentialFunction(sameDiff.f().rdiv(getFunction(this),getFunction(sameDiffVariable)))
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable add(String varName, SDVariable sameDiffVariable) {
        assertShapeEquals(sameDiffVariable);
        SDVariable ret =  SDVariable.builder()
                .sameDiff(getSameDiff())
                .varName(varName)
                .arr(null).sameDiff(sameDiffVariable.getSameDiff())
                .shape(getTransformOutputShape(sameDiffVariable))
                .differentialFunction(sameDiff.f().add(getFunction(this),getFunction(sameDiffVariable)))
                .build();

        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable sub(String varName, SDVariable sameDiffVariable) {
        assertShapeEquals(sameDiffVariable);

        DifferentialFunction left = getFunction(this);
        DifferentialFunction right = getFunction(sameDiffVariable);
        DifferentialFunction result = sameDiff.f().sub(left,right);
        SDVariable ret =  SDVariable.builder()
                .varName(varName)
                .arr(null)
                .sameDiff(sameDiffVariable.getSameDiff())
                .shape(getTransformOutputShape(sameDiffVariable))
                .differentialFunction(result)
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable div(String varName, SDVariable sameDiffVariable) {
        assertShapeEquals(sameDiffVariable);

        SDVariable ret = SDVariable.builder()
                .varName(varName)
                .arr(null).sameDiff(sameDiffVariable.getSameDiff())
                .shape(getTransformOutputShape(sameDiffVariable))
                .differentialFunction(sameDiff.f().div(getFunction(this),getFunction(sameDiffVariable)))
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable mul(String varName, SDVariable sameDiffVariable) {
        assertShapeEquals(sameDiffVariable);

        DifferentialFunction left = getFunction(this);
        DifferentialFunction right = getFunction(sameDiffVariable);
        Preconditions.checkState(left != null,"Left input is null!");
        Preconditions.checkState(right != null,"Right input is null!");

        DifferentialFunction result = sameDiff.f().mul(left,right);

        SDVariable ret = SDVariable.builder()
                .varName(varName)
                .arr(null).sameDiff(sameDiffVariable.getSameDiff())
                .shape(getTransformOutputShape(sameDiffVariable))
                .differentialFunction(result)
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }


    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable rsubi(String varName, SDVariable sameDiffVariable) {
        assertShapeEquals(sameDiffVariable);

        SDVariable ret =  SDVariable.builder()
                .varName(varName)
                .arr(null).sameDiff(sameDiffVariable.getSameDiff())
                .shape(getTransformOutputShape(sameDiffVariable))
                .differentialFunction(sameDiff.f().rsubi(getFunction(this),getFunction(sameDiffVariable)))
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable rdivi(String varName, SDVariable sameDiffVariable) {
        assertShapeEquals(sameDiffVariable);

        SDVariable ret = SDVariable.builder()
                .varName(varName)
                .arr(null).sameDiff(sameDiffVariable.getSameDiff()).shape(getTransformOutputShape(sameDiffVariable))
                .differentialFunction(sameDiff.f().rdivi(getFunction(this),getFunction(sameDiffVariable)))
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable addi(String varName, SDVariable sameDiffVariable) {
        assertShapeEquals(sameDiffVariable);

        SDVariable ret = SDVariable.builder()
                .varName(varName)
                .arr(null).sameDiff(sameDiffVariable.getSameDiff()).shape(getTransformOutputShape(sameDiffVariable))
                .differentialFunction(sameDiff.f().addi(getFunction(this),getFunction(sameDiffVariable)))
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable subi(String varName, SDVariable sameDiffVariable) {
        assertShapeEquals(sameDiffVariable);

        DifferentialFunction left = getFunction(this);
        DifferentialFunction right = getFunction(sameDiffVariable);
        DifferentialFunction result = sameDiff.f().subi(left,right);
        SDVariable ret = SDVariable.builder()
                .varName(varName)
                .arr(null)
                .sameDiff(sameDiffVariable.getSameDiff())
                .shape(getTransformOutputShape(sameDiffVariable))
                .differentialFunction(result)
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable divi(String varName, SDVariable sameDiffVariable) {
        assertShapeEquals(sameDiffVariable);

        SDVariable ret = SDVariable.builder()
                .varName(varName)
                .arr(null).sameDiff(sameDiffVariable.getSameDiff()).shape(getTransformOutputShape(sameDiffVariable))
                .differentialFunction(sameDiff.f().divi(getFunction(this),getFunction(sameDiffVariable)))
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }

    /**
     *
     * @param sameDiffVariable
     * @return
     */
    public SDVariable muli(String varName, SDVariable sameDiffVariable) {
        assertShapeEquals(sameDiffVariable);

        DifferentialFunction left = getFunction(this);
        DifferentialFunction right = getFunction(sameDiffVariable);
        DifferentialFunction result = sameDiff.getFunctionFactory().muli(left,right);
        SDVariable ret = SDVariable.builder()
                .varName(varName)
                .arr(null).sameDiff(sameDiffVariable.getSameDiff())
                .shape(getTransformOutputShape(sameDiffVariable))
                .differentialFunction(result)
                .build();
        sameDiff.addVariable(ret);
        return ret;
    }



    /**
     * Evaluate the result of this variable
     * @return
     */
    public INDArray eval() {
        SameDiff exec = sameDiff.dup();
        exec.defineFunction("output", new SameDiff.SameDiffFunctionDefinition() {
            @Override
            public SDVariable define(SameDiff sameDiff, Map<String, INDArray> inputs) {
                return SDVariable.this;
            }
        });

        SDVariable output = exec.invokeFunctionOn("output",exec);
        return output.getSameDiff().execAndEndResult();
    }





    private void assertShapeEquals(SDVariable variable) {
        if(!Arrays.equals(shape,variable.getShape()) && ArrayUtil.prod(variable.getShape()) != 1) {
            throw new IllegalArgumentException("Input shape must be the same as this shape " + Arrays.toString(shape) + " and shape was " + Arrays.toString(variable.getShape()));
        }
    }


    /**
     * Return the underlying differential
     * function
     * or array field.
     * @param variable
     * @return
     */
    public static DifferentialFunction getFunction(SDVariable variable) {
        if(variable == null)
            throw new IllegalArgumentException("Unable to get function for null variable");
        return variable.getDifferentialFunction() != null ? variable.getDifferentialFunction() : variable.getArrayField();
    }

    @Override
    public String toString() {
        return varName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SDVariable variable = (SDVariable) o;

        if (arr != null ? !arr.equals(variable.arr) : variable.arr != null) return false;
        if (arrayField != null ? !arrayField.equals(variable.arrayField) : variable.arrayField != null) return false;
        if (varName != null ? !varName.equals(variable.varName) : variable.varName != null) return false;
        if (!Arrays.equals(shape, variable.shape)) return false;
        return differentialFunction != null ? differentialFunction.equals(variable.differentialFunction) : variable.differentialFunction == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (arr != null ? arr.hashCode() : 0);
        result = 31 * result + (arrayField != null ? arrayField.hashCode() : 0);
        result = 31 * result + (varName != null ? varName.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(shape);
        result = 31 * result + (differentialFunction != null ? differentialFunction.hashCode() : 0);
        return result;
    }
}
