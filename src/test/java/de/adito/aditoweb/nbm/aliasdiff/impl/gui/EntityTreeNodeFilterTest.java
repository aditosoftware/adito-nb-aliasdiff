package de.adito.aditoweb.nbm.aliasdiff.impl.gui;

import com.google.common.graph.Traverser;
import de.adito.aditoweb.nbm.aliasdiff.dialog.PropertyNode;
import de.adito.aditoweb.system.crmcomponents.datamodels.entity.provider.IEntityProvider;
import lombok.NonNull;
import org.junit.jupiter.api.*;
import org.mockito.Answers;

import javax.swing.tree.*;
import java.util.List;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Test for {@link EntityTreeNodeFilter}
 *
 * @author w.glanzer, 09.08.2023
 * @see EntityTreeNodeFilter
 */
@SuppressWarnings("UnstableApiUsage")
class EntityTreeNodeFilterTest
{

  private static final Traverser<TreeNode> TREE_NODE_TRAVERSER = Traverser.forTree(pNode -> IntStream.range(0, pNode.getChildCount())
      .mapToObj(pNode::getChildAt)
      .collect(Collectors.toList()));

  /**
   * Test for the method {@link EntityTreeNodeFilter#filterChild(MutableTreeNode)}
   */
  @Nested
  class FilterChild
  {

    /**
     * Checks, if (transitive) children gets sorted alphabetically
     */
    @Test
    void shouldSortAlphabetically()
    {
      // Create tree structure
      DefaultMutableTreeNode root = createTreeNode("root");
      DefaultMutableTreeNode entities = createTreeNode(IEntityProvider.entities.getName());
      root.add(entities);
      entities.add(createTreeNode("node1"));
      entities.add(createTreeNode("node3"));
      entities.add(createTreeNode("node4"));
      entities.add(createTreeNode("node2"));

      // filter its children, so they should be sorted now
      List<MutableTreeNode> nodes = new EntityTreeNodeFilter().filterChild(root);
      assertEquals(1, nodes.size());
      assertEquals("root, node1, node2, node3, node4", toString(nodes.get(0)));
    }

    /**
     * Creates a readable view of the given node and its children
     *
     * @param pNode Node to get the view from
     * @return the view as string
     */
    @NonNull
    private String toString(@NonNull TreeNode pNode)
    {
      return StreamSupport.stream(TREE_NODE_TRAVERSER.breadthFirst(pNode).spliterator(), false)
          .map(TreeNode::toString)
          .collect(Collectors.joining(", "));
    }

    /**
     * Creates a {@link DefaultMutableTreeNode} with the given name
     *
     * @param pName Name that the node should have
     * @return the node
     */
    @NonNull
    private DefaultMutableTreeNode createTreeNode(@NonNull String pName)
    {
      PropertyNode node = mock(PropertyNode.class, Answers.CALLS_REAL_METHODS);
      doReturn(pName).when(node).nameForDisplay(any());
      doReturn(pName).when(node).toString();
      node.setAllowsChildren(true);
      return node;
    }

  }

}