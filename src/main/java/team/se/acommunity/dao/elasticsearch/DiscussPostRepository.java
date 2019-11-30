package team.se.acommunity.dao.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import team.se.acommunity.entity.DiscussPost;

// es也可以将其看成一个数据库，他有自己专属的注解Repository
@Repository
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost, Integer> {
    // 这个里面不用写任何东西，ElasticsearchRepository在这个类中已经实现了关于es的增删改查操作，只需要设置泛型，标识出这个要对哪一个实体类进行操作，和这个实体类的主键类型
}
