package som.primitives.arithmetic;

import java.math.BigInteger;

import som.interpreter.nodes.nary.UnaryExpressionNode;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;


@GenerateNodeFactory
public abstract class SqrtPrim extends UnaryExpressionNode {

  public SqrtPrim() { super(null); }

  private final BranchProfile longReturn   = BranchProfile.create();
  private final BranchProfile doubleReturn = BranchProfile.create();

  @Specialization
  public final Object doLong(final long receiver) {
    double result = Math.sqrt(receiver);

    if (result == Math.rint(result)) {
      longReturn.enter();
      return (long) result;
    } else {
      doubleReturn.enter();
      return result;
    }
  }

  @Specialization
  public final double doBigInteger(final BigInteger receiver) {
    return Math.sqrt(receiver.doubleValue());
  }

  @Specialization
  public final double doDouble(final double receiver) {
    return Math.sqrt(receiver);
  }
}
