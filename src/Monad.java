import org.mozilla.javascript.ast.AstNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

final public class Monad {
  /*
  public static <M,A> MonadI<M, List<A>> Sequence(
      final List<? extends MonadI<M, A>> _monads) {
    return new MonadI<M, List<A>>() {
      public <B> MonadI<M, B> Bind(
          final Function<List<A>, ? extends MonadI<M, B>> _f) {
        if (_monads.size() == 0) {
          return _f.call(Collections.<A>emptyList());
        }
        ArrayList<MonadI<M, A>> monadList = new ArrayList<MonadI<M, A>>();
        monadList.addAll(_monads);
        final List<A> _resultAs = new ArrayList<A>(monadList.size());
        int last = monadList.size()-1;
        MonadI<M, B> result = monadList.get(last).Bind(
          new Function<A, MonadI<M, B>>() {
            public MonadI<M, B> call(A a) {
              _resultAs.add(a);
              return _f.call(_resultAs);
            }
          }
        );
        for (int i=last-1; i>=0; i--) {
          MonadI<M, A> monad = monadList.get(i);
          final MonadI<M, B> _currResult = result;
          result = monad.Bind(new Function<A, MonadI<M, B>>() {
            public MonadI<M, B> call(A a) {
              _resultAs.add(a);
              return _currResult;
            }
          });
        }
        return result;
      }
    };
  }
  */

  public static Generator SequenceBind(
      List<Generator> monads,
      Function<List<AstNode>, Generator> f) {
    return SequenceBind(monads.iterator(), f, new Cons.Nil<AstNode>());
  }

  public static Generator SequenceBind(
      final Iterator<Generator> _monads,
      final Function<List<AstNode>, Generator> _f,
      final Cons<AstNode> _reversedValues) {
    if (_monads.hasNext()) {
      return _monads.next().Bind(new Function<AstNode, Generator>() {
        public Generator call(AstNode a) {
          return Monad.SequenceBind(
            _monads,
            _f,
            new Cons<AstNode>(a, _reversedValues)
          );
        }
      });
    } else {
      return _f.call(new LinkedList<AstNode>() {{
        for (AstNode a : _reversedValues) {
          addFirst(a);
        }
      }});
    }
  }

  public static Generator Bind2(
      final Generator _a,
      final Generator _b,
      final Function<Pair<AstNode,AstNode>, Generator> _f) {
    return SequenceBind(
      new LinkedList<Generator>() {{
        add(_a);
        add(_b);
      }},
      new Function<List<AstNode>, Generator>() {
        public Generator call(List<AstNode> list) {
          Iterator<AstNode> it = list.iterator();
          return _f.call(new Pair<AstNode,AstNode>(
            it.next(),
            it.next()
          ));
        }
      }
    );
  }

  public static Generator Bind3(
      final Generator _a,
      final Generator _b,
      final Generator _c,
      final Function<Pair<AstNode,Pair<AstNode,AstNode>>, Generator> _f) {
    return SequenceBind(
      new LinkedList<Generator>() {{
        add(_a);
        add(_b);
        add(_c);
      }},
      new Function<List<AstNode>, Generator>() {
        public Generator call(List<AstNode> list) {
          Iterator<AstNode> it = list.iterator();
          return _f.call(new Pair<AstNode,Pair<AstNode,AstNode>>(
            it.next(),
            new Pair<AstNode,AstNode>(
              it.next(),
              it.next()
            )
          ));
        }
      }
    );
  }
}
