package team.se.acommunity;

import com.mysql.cj.QueryBindings;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import team.se.acommunity.dao.DiscussPostMapper;
import team.se.acommunity.dao.elasticsearch.DiscussPostRepository;
import team.se.acommunity.entity.DiscussPost;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = AgriculturalCommunityApplication.class)
public class ElasticsearchTest {
    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    // 添加一个数据
    @Test
    public void testInsert() {
        discussPostRepository.save(discussPostMapper.getDiscussPostById(121));
        discussPostRepository.save(discussPostMapper.getDiscussPostById(122));
        discussPostRepository.save(discussPostMapper.getDiscussPostById(123));

    }

    // 添加多个数据
    @Test
    public void testInsertList() {
        discussPostRepository.saveAll(discussPostMapper.listDiscussPosts(101, 0, 100, 0));
        discussPostRepository.saveAll(discussPostMapper.listDiscussPosts(102, 0, 100, 0));
        discussPostRepository.saveAll(discussPostMapper.listDiscussPosts(103, 0, 100, 0));
        discussPostRepository.saveAll(discussPostMapper.listDiscussPosts(111, 0, 100, 0));
        discussPostRepository.saveAll(discussPostMapper.listDiscussPosts(112, 0, 100, 0));
    }

    // 修改数据
    @Test
    public void testUpdate() {
        DiscussPost post = discussPostMapper.getDiscussPostById(231);
        post.setContent("我是新人，大家好");
        // es的修改实际上就是覆盖
        discussPostRepository.save(post);
    }

    // 删除
    @Test
    public void testDelete() {
        // 删除一条
        discussPostRepository.deleteById(231);
        // 删掉所有数据
        discussPostRepository.deleteAll();
    }

    // 利用Repository进行搜索
    @Test
    public void testSearchByRepository() {
        // SearchQuery这个是spring提供给的搜索配置类
        // NativeSearchQueryBuilder这个方法可以构建一个搜索配置类
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content")) // 这个方法multiMatchQuery表示进行多字段搜索，对title和content都搜索这个互联网寒冬
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC)) // 构造排序的条件  按照type为标准进行倒叙排序   type表示的是这个帖子是否指定，这样指定的帖子就会放到前面去
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC)) // 如果type字段的条件一样，就对score字段为标准进行排序
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC)) // 如果上面两个字段都一样，就按照创建时间来排序
                .withPageable(PageRequest.of(0, 10)) // 设置分页查询
                .withHighlightFields( // 指定哪些词进行高亮显示，这边高亮显示就是在词的外面套一个标签，也可以指定套一个什么标签   下面就设置和title匹配的词和content匹配的词要加高亮标签
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        // elasticTemplate.queryForPage(searchQuery, class, SearchResultMapper)
        // 底层获取得到了高亮显示的值, 但是没有返回.解决这个问题要么重写elasticTemplate这个方法，要么直接用elasticsearchTemplate去进行es操作

        // Page是spring自带的es分页配置类,就是说page就是一个集合，里面存了多个DiscussPost的实体对象，通过discussPostRepository查询出来的数据都存到了这个page集合中
        Page<DiscussPost> page = discussPostRepository.search(searchQuery);
        // 一共查到了多少数据
        System.out.println(page.getTotalElements());
        // 一共有多少页
        System.out.println(page.getTotalPages());
        // 当前处在第几页
        System.out.println(page.getNumber());
        // 每一页显示多少条数据
        System.out.println(page.getSize());
        for (DiscussPost post : page) {
            System.out.println(post);
        }
    }

    // 利用Template进行搜索
    @Test
    public void testSearchByTemplate() {
        // 配置和上面那个是一样的
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        // queryForPage方法需要传入三个参数，一个配置对象searchQuery，一个search类型DiscussPost.class，第三个是SearchResultMapper这个接口，在这个接口里面去实现高亮标签与返回数据合并。queryForPage这个方法或得到结果，然后得到的结果会自动交给SearchResultMapper去处理
        Page<DiscussPost> page = elasticsearchTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {
                // 获得返回数据，SearchHits相当于一个集合，存储返回的数据
                SearchHits hits = response.getHits();
                // 判断数据量，如果没有数据就结束
                if (hits.getTotalHits() <= 0) {
                    return null;
                }

                List<DiscussPost> list = new ArrayList<>();
                for (SearchHit hit : hits) {
                    DiscussPost post = new DiscussPost();

                    // hit中的数据是以json格式的map类型，先使用getSourceAsMap方法将其转换成map对象，再通过get得到value
                    String id = hit.getSourceAsMap().get("id").toString();
                    // 得到的数据都是字符串，需要自己转成相应的类型
                    post.setId(Integer.valueOf(id));

                    String userId = hit.getSourceAsMap().get("userId").toString();
                    post.setUserId(Integer.valueOf(userId));

                    // 这里面是原始的title数据，还没有高亮标签
                    String title = hit.getSourceAsMap().get("title").toString();
                    post.setTitle(title);

                    // 这里面是原始的content数据，还没有高亮标签
                    String content = hit.getSourceAsMap().get("content").toString();
                    post.setContent(content);

                    String status = hit.getSourceAsMap().get("status").toString();
                    post.setStatus(Integer.valueOf(status));

                    String createTime = hit.getSourceAsMap().get("createTime").toString();
                    // 先变成数，在变成日期格式
                    post.setCreateTime(new Date(Long.valueOf(createTime)));

                    String commentCount = hit.getSourceAsMap().get("commentCount").toString();
                    post.setCommentCount(Integer.valueOf(commentCount));

                    // 处理高亮显示的结果
                    // 查看有没有返回高亮显示的内容
                    HighlightField titleField = hit.getHighlightFields().get("title");
                    if (titleField != null) {
                        // 如果有就将高亮内容覆盖掉原始的数据  titleField.getFragments()的到高亮的数据，返回数组类型，因为搜到的题目中可能对关键字多个匹配，互联网寒冬即匹配了互联网，这是一个高亮词，就是<em>互联网</em>寒冬  又匹配了寒冬 互联网<em>寒冬</em>,，但是我们只要数组的第一个匹配就行了，没必要全都取出来
                        post.setTitle(titleField.getFragments()[0].toString());
                    }
                    // 同上
                    HighlightField contentField = hit.getHighlightFields().get("content");
                    if (contentField != null) {
                        post.setContent(contentField.getFragments()[0].toString());
                    }

                    list.add(post);
                }

                // 返回AggregatedPageImpl类型的对象，还需要将其他相关数据传入，有数据集合list,方法参数pageable，还有一共多少条数据等等一系列的数据
                return new AggregatedPageImpl(list, pageable,
                        hits.getTotalHits(), response.getAggregations(), response.getScrollId(), hits.getMaxScore());
            }
        });

        System.out.println(page.getTotalElements());
        System.out.println(page.getTotalPages());
        System.out.println(page.getNumber());
        System.out.println(page.getSize());
        for (DiscussPost post : page) {
            System.out.println(post);
        }
    }
}
