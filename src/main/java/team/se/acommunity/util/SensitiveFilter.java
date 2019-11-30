package team.se.acommunity.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {
    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);
    // 替换符
    private static final String REPLACEMENT = "**";

    // 根结点
    private TrieNode rootNode = new TrieNode();

    /**
     * 下面这个标签表示的是定义一个初始化方法，在这个bean第一次被容器初始化的时候，会调用这个初始化方法
     * bean被容器初始化就是服务启动的时候，就是已启动spring项目，spring容器就会把虽有的bean全部装载进去，这个时候bean就会被初始化，就会执行这个初始化方法
     */
    @PostConstruct
    public void init() {
        // this.getClass是根据对象来获取类
        // getClassLoader()是获取类的加载器,类加载器就是指的target目录中的classes文件夹，所有编译好的文件，静态资源等等都会在编译项目之后被输出到这里
        // getResourceAsStream()这个方法就是要获得getClassLoader()这个目录中"sensitive-words.txt"这个文件的内容的字节流
        try (
                // 因为这个输入流最后要关闭的，所以这里就用try来把他括起来
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                // 得到的is是一个字节流，字节流取内容不太方便，就要将他转化成字符流，使用InputStreamReader
                // 再将字符流变成缓冲流，这样获取数据的时候效率比较高
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                // 这里用了一种try的写法，在try后面加了一个括号，因为这个is，reader最后应该给close掉的，把这个is,reader定义在了这个()里面
                // 最后系统会自动执行一个finally将这个is给close掉，这样写的好处就是因为这个is是定义在try当中的，如果用以前那种手动编写finally的方法，在finally代码块中是没有这个is对象的，就得把is这个对象定义在try外面，比较麻烦
        ) {
            // 这个里面就写try中的逻辑代码
            String keyword;
            // reader.readLine()表示读一行，然后将指针指向下一行
            while ((keyword = reader.readLine()) != null) {
                // 添加到前缀树
                this.addKeyword(keyword);
            }
        } catch (IOException e) {
            logger.error("加载敏感词文件失败: " + e.getMessage());
        }
    }

    // 将一个敏感词加入到前缀树中 这是一个私用方法，只在这个类中调用
    private void addKeyword(String keyword) {
        // 将指针指向根结点
        TrieNode tempNode = rootNode;
        // 遍历字符串，将字符串内容添加到前缀树当中
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);
            // 如果当前结点没有这个c字符的子结点，就为他创建这个子结点
            if (subNode == null) {
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }

            // 指向子结点，进入下一轮循环
            tempNode = subNode;

            // 设置结束标识符
            if (i == keyword.length() - 1) {
                tempNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词汇，这个方法是要被外界调用的，所以权限是public
     * @param text 待过滤的文本
     * @return 过滤后的文本
     */
    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }

        // 指针1
        TrieNode tempNode = rootNode;
        // 指针2
        int begin = 0;
        // 指针3
        int position = 0;
        // 结果
        StringBuilder sb = new StringBuilder();

        while (begin < text.length()) {
            char c = text.charAt(position);

            // 跳过符号
            if (isSymbol(c)) {
                // 若指针1属于根结点，将此符号计入结果，让指针2向下走一步
                if (tempNode == rootNode) {
                    sb.append(c);
                    begin++;
                }
                // 无论符号在开头或中间，指针3都向下走一步
                position++;
                // 前缀树上的指针不动，字符串上的指针动，跳过特殊符号查看后面的符号
                continue;
            }

            // 检查下级结点
            tempNode = tempNode.getSubNode(c);
            if (tempNode == null) {
                // 说明前缀树中没有存储当前这个字符串的敏感词汇，以begin为开头的字符串不是敏感词
                sb.append(text.charAt(begin));
                // 进入下一个位置
                position = ++begin;
                // 重新指向根结点
                tempNode = rootNode;
            } else if (tempNode.isKeywordEnd()) {
                // 发现了敏感词，将begin-position字符串替换掉
                sb.append(REPLACEMENT);
                begin = ++position;
                tempNode = rootNode;
            } else {
                // 检查下一个字符
                if (position < text.length() - 1) {
                    position++;
                } else if (position == text.length() - 1) {
                    // 这里单独讨论了一下具体情况，具体原因在评论区有
                    // 当已经检查到字符串最后一个字符的时候，而这个字符串也不是敏感词汇，那么就执行一次不是敏感词汇的操作
                    sb.append(text.charAt(begin));
                    position = ++begin;
                    tempNode = rootNode;
                }
            }
        }

        return sb.toString();
    }

    // 判断是否为符号，是特殊符号返回true
    private boolean isSymbol(Character c) {
        // 这个方法如果字符是普通字符(如ABC)就返回true,如果是特殊符号（如@#$），就返回false
        // 0x2E80-0x9FFF是东亚文字范围，包括中文，日文，韩文，因为对于西方人来说只要不是英文字母就都是特殊符号，但是咱们开发的是中文项目，所以东亚文字范围的符号也应该是普通字符
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }
    // 前缀树  定义的是一个内部类，因为这个前缀树对象只在这个类中使用，所以直接定义一个内部类就可以了
    private class TrieNode {
        // 关键字结束标识
        private boolean isKeywordEnd = false;

        // 子结点(key是下级结点字符，value是下级结点)
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        // 添加子结点
        public void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }

        // 根据字符获取对应的子结点
        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }
    }
}
