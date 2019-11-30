package team.se.acommunity.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import team.se.acommunity.entity.DiscussPost;
import team.se.acommunity.entity.Page;
import team.se.acommunity.service.ElasticsearchService;
import team.se.acommunity.service.LikeService;
import team.se.acommunity.service.UserService;
import team.se.acommunity.util.CommunityConstant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {
    @Autowired
    private ElasticsearchService elasticsearchService;

    // 将贴子搜索出来之后需要展示帖子的作者和点赞数量，所以还要将UserService和LikeService注入进来
    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    // get请求方式只能在请求头中传输，不能存放在请求体中，所以keyword就只能拼在访问路径中传过来
    // search?keyword=xxx
    @RequestMapping(path = "/search", method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model) {
        // 搜索文章  搜索页数从0页开始，所以要-1
        // 下面这个spring自带的page对象中存储搜索结果
        org.springframework.data.domain.Page<DiscussPost> searchResult =
        elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());

        // 聚合数据
        List<Map<String, Object>> dicussPosts = new ArrayList<>();
        if (searchResult != null) {
            for (DiscussPost post : searchResult) {
                Map<String, Object> map = new HashMap<>();
                // 帖子
                map.put("post", post);
                // 作者
                map.put("user", userService.getUserById(post.getUserId()));
                // 点赞数量
                map.put("likeCount", likeService.countEntityLike(ENTITY_TYPE_POST, post.getId()));

                dicussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", dicussPosts);
        // 界面点击完搜索之后还应该显示着搜索的关键词，所以要将keyword传过去
        model.addAttribute("keyword", keyword);

        // 分页信息
        page.setPath("/search?keyword=" + keyword);
        page.setRows(searchResult == null ? 0 : searchResult.getTotalPages());

        return "/site/search";
    }
}
