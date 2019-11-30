$(function(){
	// 对发布按钮做一个点击事件，点了发布按钮之后就调用publish()方法
	$("#publishBtn").click(publish);
});

function publish() {
	// 点完了发布按钮，将发布文章页面隐藏
	$("#publishModal").modal("hide");

	// 发送ajax请求之前，将CSRF令牌设置到请求的消息头中
	// 下面的意思是获取name为_csrf的meta标签，并且获取content属性的值
	// var token = $("meta[name='_csrf']").attr("content");
	// var header = $("meta[name='_csrf_header']").attr("content");
	// // 设置ajax的请求头
	// $(document).ajaxSend(function (e, xhr, options) {
	// 	// xhr是ajax的核心实现对象，通过它来设置请求头，设置为key是header  value是token
	// 	xhr.setRequestHeader(header, token);
	// });

	// 获取标题和内容
	// 取标题的标签 这里使用的是jquery的ID选择器，将界面中id值是recipient-name的标签获取，然后用val方法获取标签的value值
	var title = $("#recipient-name").val();
	var content = $("#message-text").val();
	// 发送异步请求（POST）相关笔记在HTML文件夹的demo页面里面
	$.post(
		CONTEXT_PATH + "/discuss/add",
		{"title":title,"content":content},
		// 添加回调函数，处理返回结果
		function (data) {
			// 根据json格式的字符串创建js对象
			data = $.parseJSON(data);
			// 在提示框当中显示提示消息
			// id选择器获取这个提示框标签,利用text方法修改一下这个便签里面的文本
			$("#hintBody").text(data.msg);
			// 显示提示消息
			$("#hintModal").modal("show");
			// 2秒后自动隐藏提示框
			setTimeout(function(){
				$("#hintModal").modal("hide");
				// 刷新页面
				if (data.code === 0) { // 判断是否已经发表成功
					window.location.reload();
				}
			}, 2000);
		}
	);

}