package som.interpreter.nodes.specialized.whileloops;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vm.constants.Globals;
import som.vm.constants.Nil;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;
import som.vmobjects.SObject;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;


public abstract class WhileCache extends BinaryExpressionNode {

  public static final int INLINE_CACHE_SIZE = 6;

  protected final boolean predicateBool;

  public WhileCache(final boolean predicateBool) {
    this.predicateBool = predicateBool;
  }

  @Specialization(limit = "INLINE_CACHE_SIZE",
      guards = {"loopCondition.getMethod() == cachedLoopCondition",
                "loopBody.getMethod() == cachedLoopBody"})
  public final SObject doCached(final VirtualFrame frame,
      final SBlock loopCondition, final SBlock loopBody,
      @Cached("loopCondition.getMethod()") final SInvokable cachedLoopCondition,
      @Cached("loopBody.getMethod()") final      SInvokable cachedLoopBody,
      @Cached("create(loopCondition, loopBody, predicateBool)") final
         WhileWithDynamicBlocksNode whileNode) {
    return whileNode.doWhileUnconditionally(frame, loopCondition, loopBody);
  }

  private boolean obj2bool(final Object o) {
    if (o instanceof Boolean) {
      return (boolean) o;
    } else if (o == Globals.trueObject) {
      CompilerAsserts.neverPartOfCompilation("obj2Bool1");
      return true;
    } else {
      CompilerAsserts.neverPartOfCompilation("obj2Bool2");
      assert o == Globals.falseObject;
      return false;
    }
  }

  @Specialization(contains = "doCached")
  public final SObject doUncached(final VirtualFrame frame, final SBlock loopCondition,
      final SBlock loopBody) {
    CompilerAsserts.neverPartOfCompilation("WhileCache.GenericDispatch"); // no caching, direct invokes, no loop count reporting...

    Object conditionResult = loopCondition.getMethod().invoke(loopCondition);

    // TODO: this is a simplification, we don't cover the case receiver isn't a boolean
    boolean loopConditionResult = obj2bool(conditionResult);

    // TODO: this is a simplification, we don't cover the case receiver isn't a boolean
    while (loopConditionResult == predicateBool) {
      loopBody.getMethod().invoke(loopBody);
      conditionResult = loopCondition.getMethod().invoke(loopCondition);
      loopConditionResult = obj2bool(conditionResult);
    }
    return Nil.nilObject;
  }
}
