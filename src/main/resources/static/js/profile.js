$(function(){
	$(".follow-btn").click(follow);
});

function follow() {
	var btn = this;
	// 这里是通过按钮的样式来判断是不是已经关注了，如果按钮样式是蓝色，说明未关注，按钮的样式是灰色说明关注了
	if($(btn).hasClass("btn-info")) {
		// 关注TA
		$.post (
			CONTEXT_PATH + "/follow",
			// 3表示的实体类型是人，所以这个操作是关注人  第二个参数是获得当前标签btn的上一个标签里面的值，上一个标签是隐藏表示，为的就是将用户id传给js来用
			{"entityType":3,"entityId":$(btn).prev().val()},
			function(data) {
				data = $.parseJSON(data);
				if (data.code == 0) {
					// 让界面重新刷新一下
					window.location.reload();
				} else {
					alert(data.msg);
				}
			}
		);
		// $(btn).text("已关注").removeClass("btn-info").addClass("btn-secondary");
	} else {
		// 取消关注
		$.post (
			CONTEXT_PATH + "/unfollow",
			// 3表示的实体类型是人，所以这个操作是关注人  第二个参数是获得当前标签btn的上一个标签里面的值，上一个标签是隐藏表示，为的就是将用户id传给js来用
			{"entityType":3,"entityId":$(btn).prev().val()},
			function(data) {
				data = $.parseJSON(data);
				if (data.code == 0) {
					// 让界面重新刷新一下
					window.location.reload();
				} else {
					alert(data.msg);
				}
			}
		);
		// $(btn).text("关注TA").removeClass("btn-secondary").addClass("btn-info");
	}
}