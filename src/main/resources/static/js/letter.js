$(function(){
	$("#sendBtn").click(send_letter);
	$(".close").click(delete_msg);
});
// 这个是发送私信的方法，对发送私信按钮加了一个单击事件，点击会触发这个方法
function send_letter() {
	// 将对话框隐藏掉
	$("#sendModal").modal("hide");
	// 使用Id选择器获得发送对象和私信内容
	var toName = $("#recipient-name").val();
	var content = $("#message-text").val();
	// 异步发送一个post请求
	$.post (
		CONTEXT_PATH + "/letter/send",
		{"toName":toName, "content":content},
		function (data) {
			// 处理服务器返回来的内容，通过jquery将json转变成js对象
			data = $.parseJSON(data);
			if (data.code == 0) {
				// .text修改标签的文本内容
				$("#hintBody").text("发送成功！");
			} else {
				// 固定的用法，data.msg就是取得服务器响应回来的数据
				$("#hintBody").text(data.msg);
			}

			// 将提示框显示出来
			$("#hintModal").modal("show");
			// 2秒之后将提示框关掉
			setTimeout(function(){
				$("#hintModal").modal("hide");
				// 重新刷新当前页面,因为发送完私信之后私信列表就应该有新的信息了，所以要刷新一下页面
				location.reload();
			}, 2000);
		}
	)

}

function delete_msg() {
	// TODO 删除数据
	$(this).parents(".media").remove();
}