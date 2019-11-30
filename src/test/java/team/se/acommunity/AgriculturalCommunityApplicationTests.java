package team.se.acommunity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import sun.java2d.pipe.SpanShapeRenderer;
import team.se.acommunity.dao.AlphaDao;
import team.se.acommunity.service.AlphaService;

import java.text.SimpleDateFormat;
import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
// 下面这个标签表示的是让这个类作为配置类
@ContextConfiguration(classes = AgriculturalCommunityApplication.class)
// 如果想获得spring对象，就让这个类实现ApplicationContextAware这个接口
public class AgriculturalCommunityApplicationTests implements ApplicationContextAware {
	// 这个对象，就是用来存储spring容器对象
	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		// 这个方法能自动获取spring容器对象，用自己创建的属性来保存一下这个spring对象
		this.applicationContext = applicationContext;
	}

	@Test
	public void testApplicationContext() {
		System.out.println(applicationContext);

		// 从spring容器中获取自动装配的AlphaDaoHibernate这个bean
		AlphaDao alphaDao = applicationContext.getBean(AlphaDao.class);
		System.out.println(alphaDao.select());

		// 还可以通过bean名获取对象，而不是通过类型来获取对象，第二个参数声明要返回一个什么类型的对象，否则返回Object
		alphaDao = applicationContext.getBean("alphaH", AlphaDao.class);
		System.out.println(alphaDao.select());
	}

	// 通过观察在spring容器中bean的生命周期，发现同一个类型的对象在spring中只被实例化销毁一次，它是单例的
	// 代码获取了两次，但是通过控制台输出发现只被实例化了一次，说明在spring容器中的bean是单例的
	@Test
	public void testBeanManagement() {
		AlphaService alphaService = applicationContext.getBean(AlphaService.class);
		System.out.println(alphaService);

		alphaService = applicationContext.getBean(AlphaService.class);
		System.out.println(alphaService);
	}

	@Test
	public void testBeanConfig() {
		SimpleDateFormat simpleDateFormat = applicationContext.getBean(SimpleDateFormat.class);
		System.out.println(simpleDateFormat.format(new Date()));
	}

	// 上面讲的这些属于主动从spring容器中获取bean，是为了让大家理解spring容器是个什么东西
	// 真正实际使用的时候不用上面这么麻烦，直接用下面这个注解就可以
	/**
	 * 下面这个注解的意思是告诉spring容器把AlphaDao的bean注入给alphaDao这个对象，这就是spring boot的依赖注入
	 */
	@Autowired
	// 下面这个注释就是指定将名字叫做alphaH的这个bean注入给alohaDao，而且这个标签只在这一次依赖注入有效，其他位置的注入还是会使用默认的设置
	@Qualifier("alphaH")
	private AlphaDao alphaDao;

	@Autowired
	private AlphaService alphaService;

	@Autowired
	private SimpleDateFormat simpleDateFormat;

	@Test
	public void testDI() {
		System.out.println(alphaDao);
		System.out.println(alphaService);
		System.out.println(simpleDateFormat);
	}
}
