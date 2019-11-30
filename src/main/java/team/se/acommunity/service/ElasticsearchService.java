package team.se.acommunity.service;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;
import team.se.acommunity.dao.elasticsearch.DiscussPostRepository;
import team.se.acommunity.entity.DiscussPost;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ElasticsearchService {
    // 用来对es进行增删改查操作
    @Autowired
    private DiscussPostRepository discussPostRepository;
    // 用来添加高亮显示操作
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    // 存入新数据/修改数据
    public void saveDiscussPost(DiscussPost post) {
        discussPostRepository.save(post);
    }

    // 删除数据
    public void removeDiscussPost(int id) {
        discussPostRepository.deleteById(id);
    }

    /**
     * 搜索方法
     * @param keyword 搜索关键字
     * @param current 当前是第几页
     * @param limit 一页显示多少数据
     * @return
     */
    public Page<DiscussPost> searchDiscussPost(String keyword, int current, int limit) {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword, "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(current, limit)) // 设置分页条件
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        // queryForPage方法需要传入三个参数，一个配置对象searchQuery，一个search类型DiscussPost.class，第三个是SearchResultMapper这个接口，在这个接口里面去实现高亮标签与返回数据合并。queryForPage这个方法或得到结果，然后得到的结果会自动交给SearchResultMapper去处理
        return elasticsearchTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
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
    }
}
