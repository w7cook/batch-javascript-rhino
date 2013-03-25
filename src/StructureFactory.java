import batch.partition.PartitionFactory;
import batch.Op;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StructureFactory implements PartitionFactory<StructureFactory.Item> {
  public static class Item {
    public final Tag tag;
    public final Object value;
    public final Object extraInfo;
    public final Item[] args;

    private Item(Tag tag, Object value, Object extraInfo, Item... args) {
      this.tag = tag;
      this.value = value;
      this.extraInfo = extraInfo;
      this.args = args;
    }

    public static Item Raw(Object value) {
      return new Item(Tag.RAW, value, null);
    }

    public Item(Tag tag, Item... args) {
      this(tag, null, null, args);
    }

    public Item(Item i, Object extraInfo) {
      this(i.tag, i.value, extraInfo, i.args);
    }
    
    public boolean equals(Object o) {
      if (!(o instanceof Item)) {
        return false;
      } else {
        Item other = (Item)o;
        return tag.equals(Tag.ANY)
            || other.tag.equals(Tag.ANY)
            || (
                 tag.equals(other.tag)
              && (extraInfo==other.extraInfo
                  || (extraInfo != null && extraInfo.equals(other.extraInfo)))
              && (value==other.value
                  || (value != null && value.equals(other.value)))
              && Arrays.equals(args, other.args)
            );
      }
    }

    public String toString() {
      String s = tag + "(";
      if (value != null) {
        s += value;
      }
      if (args != null) {
        for (int i=0; i<args.length-1; i++) {
          s += args[i] + ", ";
        }
        if (args.length > 0) {
          s += args[args.length-1];
        }
      }
      s += ")";
      if (extraInfo != null) {
        s += ".setExtra(" + extraInfo + ")";
      }
      return s;
    }

    private static final StructureFactory sf = new StructureFactory();
    public Item reduce() {
      if (this.equals(sf.Prim(Op.SEQ, sf.Any()))) {
        return this.args[1].reduce();
      }
      Item[] newArgs = null;
      if (args != null) {
        newArgs = new Item[args.length];
        for (int i=0; i<args.length; i++) {
          newArgs[i] = args[i].reduce();
        }
      }
      return new Item(tag, value, extraInfo, newArgs);
    }
  }

  public static enum Tag {
    ANY,

    VAR,
    DATA,
    FUN,
    PRIM,
    PROP,
    ASSIGN,
    LET,
    IF,
    LOOP,
    CALL,
    IN,
    OUT,
    ROOT,
    SKIP,
    OTHER,
    DYNAMIC_CALL,
    MOBILE,

    RAW
  }

  public static final Item[] EMPTY_ARRAY = new Item[] {};

  public Item Any() {
    return new Item(Tag.ANY);
  }

  @Override
  public Item Var(String name) {
    return new Item(Tag.VAR, Item.Raw(name));
  }

  @Override
  public Item Data(Object v) {
    return new Item(Tag.DATA, Item.Raw(v));
  }

  @Override
  public Item Fun(String var, Item body) {
    return new Item(Tag.FUN, Item.Raw(var), body);
  }

  @Override
  public Item Prim(Op op, List<Item> args) {
    List<Item> newArgs = new ArrayList<Item>(args.size()+1);
    newArgs.add(Item.Raw(op));
    newArgs.addAll(args);
    return new Item(Tag.PRIM, newArgs.<Item>toArray(EMPTY_ARRAY));
  }

  @Override
  public Item Prop(Item base, String field) {
    return new Item(Tag.PROP, base, Item.Raw(field));
  }

  @Override
  public Item Assign(Item target, Item source) {
    return new Item(Tag.ASSIGN, target, source);
  }

  @Override
	public Item Let(String var, Item expression, Item body) {
    return new Item(Tag.LET, Item.Raw(var), expression, body);
  }

  @Override
	public Item If(Item condition, Item thenExp, Item elseExp) {
    return new Item(Tag.IF, condition, thenExp, elseExp);
  }

  @Override
	public Item Loop(String var, Item collection, Item body) {
    return new Item(Tag.LOOP, Item.Raw(var), collection, body);
  }

  @Override
	public Item Call(Item target, String method, List<Item> args) {
    List<Item> newArgs = new ArrayList<Item>(args.size()+2);
    newArgs.add(target);
    newArgs.add(Item.Raw(method));
    newArgs.addAll(args);
    return new Item(Tag.CALL, newArgs.<Item>toArray(EMPTY_ARRAY));
  }

  @Override
	public Item In(String location) {
    return new Item(Tag.IN, Item.Raw(location));
  }

  @Override
	public Item Out(String location, Item expression) {
    return new Item(Tag.OUT, Item.Raw(location), expression);
  }

  @Override
	public String RootName() {
    throw new Error("Root Name is not defined");
  }

  @Override
	public Item Root() {
    return new Item(Tag.ROOT);
  }

  @Override
	public Item Prim(Op op, Object... args) {
    List<Item> list = new ArrayList<Item>(args.length);
    for (Object o : args) {
      list.add((Item)o);
    }
    return Prim(op, list);
  }

  @Override
	public Item Call(Item target, String method, Object... args) {
    List<Item> list = new ArrayList<Item>(args.length);
    for (Object o : args) {
      list.add((Item)o);
    }
    return Call(target, method, list);
  }

  @Override
	public Item Skip() {
    return new Item(Tag.SKIP);
  }

  @Override
	public Item Other(Object external, Item... subs) {
    List<Item> args = new ArrayList<Item>(subs.length+1);
    args.add(Item.Raw(external));
    args.addAll(Arrays.asList(subs));
    return new Item(Tag.OTHER, args.<Item>toArray(EMPTY_ARRAY));
  }

  @Override
	public Item Other(Object external, List<Item> subs) {
    return Other(external, subs.<Item>toArray(EMPTY_ARRAY));
  }

  @Override
	public Item DynamicCall(Item target, String method, List<Item> args) {
    List<Item> newArgs = new ArrayList<Item>(args.size()+2);
    newArgs.add(target);
    newArgs.add(Item.Raw(method));
    newArgs.addAll(args);
    return new Item(Tag.DYNAMIC_CALL, newArgs.<Item>toArray(EMPTY_ARRAY));
  }

  @Override
	public Item Mobile(String type, Item exp) {
    return new Item(Tag.MOBILE, Item.Raw(type), exp);
  }
	
  @Override
	public Item setExtra(Item exp, Object extra) {
    return new Item(exp, extra);
  }

}
