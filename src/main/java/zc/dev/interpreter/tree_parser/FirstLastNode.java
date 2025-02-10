package zc.dev.interpreter.tree_parser;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor(staticName = "from")
public class FirstLastNode {
    private final TreeNode firstNode;
    private final TreeNode lastNode;
}
