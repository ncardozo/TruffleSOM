package som.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;

import som.interpreter.SArguments;
import som.primitives.ObjectPrims.ClassPrim;
import som.primitives.ObjectPrimsFactory.ClassPrimFactory;
import som.vm.Universe;
import som.vmobjects.SArray;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SSymbol;

public final class GenericDispatchNode extends AbstractDispatchNode {
  @Child private IndirectCallNode call;
  @Child private ClassPrim getClass;
  protected final SSymbol selector;

  public GenericDispatchNode(final SSymbol selector) {
    this.selector = selector;
    call = Truffle.getRuntime().createIndirectCallNode();
    getClass = ClassPrimFactory.create(null);
  }

  @Override
  public Object executeDispatch(
      final VirtualFrame frame, final Object[] arguments) {
    Universe.callerNeedsToBeOptimized("We should not reach this, ideally, in benchmark code");

    Object rcvr = arguments[0];
    DynamicObject rcvrClass = (DynamicObject) getClass.executeEvaluated(null, rcvr);
    SInvokable method = SClass.lookupInvokable(rcvrClass, selector);

    CallTarget target;
    Object[] args;

    if (method != null) {
      target = method.getCallTarget();
      args = arguments;
    } else {
      // Won't use DNU caching here, because it is already a megamorphic node
      SArray argumentsArray = SArguments.getArgumentsWithoutReceiver(arguments);
      args = new Object[] {arguments[0], selector, argumentsArray};
      target = CachedDnuNode.getDnuCallTarget(rcvrClass);
    }
    return call.call(frame, target, args);
  }

  @Override
  public int lengthOfDispatchChain() {
    return 1000;
  }
}
