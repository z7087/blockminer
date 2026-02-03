package me.z7087.blockminer.util.data;
/*
import java.util.Objects;

// 我忘记我搞这个要做什么了
@Deprecated
public class LinkedNode {
    final LinkedNode lastNode;
    final Object key;
    final transient int hash;

    LinkedNode(LinkedNode lastNode, Object key) {
        this.lastNode = lastNode;
        this.key = Objects.requireNonNull(key);

        this.hash = lastNode == null
                ? key.hashCode()
                : (lastNode.hashCode() * 31 + key.hashCode());
    }

    public static LinkedNode of(Object key) {
        return new LinkedNode(null, key);
    }

    public LinkedNode then(Object key) {
        return new LinkedNode(this, key);
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof LinkedNode)) return false;

        LinkedNode thisNode = this;
        LinkedNode thatNode = (LinkedNode) o;
        do {
            if (thisNode == thatNode)
                return true;
            if (!thisNode.key.equals(thatNode.key))
                return false;
            thisNode = thisNode.lastNode;
            thatNode = thatNode.lastNode;
        } while (thisNode != null && thatNode != null);
        return thisNode == thatNode;
    }

    @Override
    public final int hashCode() {
        return hash;
    }
}
*/
