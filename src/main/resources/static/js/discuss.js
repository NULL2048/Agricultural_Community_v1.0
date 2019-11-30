// 下面这种写法就相当于原生Js中的window.onload  就是说自动在界面加载完成后触发$()里面的js方法
$(function () {
    // 一致监听点击按钮事件，触发对应的方法
    $("#topBtn").click(setTop);
    $("#wonderfulBtn").click(setWonderful);
    $("#deleteBtn").click(setDelete);

})

// 这个方法就是一个ajax方法
function like(btn, entityType, entityId, entityUserId, postId) {
    $.post(
        // 数据要传送给的地址
        CONTEXT_PATH + "/like",
        // 要传给服务器的数据
        {"entityType":entityType,"entityId":entityId,"entityUserId":entityUserId,"postId":postId},
        // 接收服务器响应数据
        function(data) {
            // 将json格式转化为js对象
            data = $.parseJSON(data);

            if (data.code == 0) {
                // 通过界面传过来的当前标签，可以取到它的子标签，来修改其内容
                $(btn).children("i").text(data.likeCount);
                $(btn).children("b").text(data.likeStatus==1?'已赞':"赞");
            } else {
                // 这里并没有什么问题，在当时统一异常处理的位置如果出现服务器报错，已经处理了向异步请求响应回去一个“服务器异常”消息，所以这里的msg一定是有内容的
                alert(data.msg);
            }
        }
    );
}

// 置顶
function setTop() {
    $.post(
        CONTEXT_PATH + "/discuss/top",
        // 取得隐藏标签中的postId
        {"id":$("#postId").val()},
        // 回调函数 处理服务器响应结果
        function (data) {
            // 将json转成js对象
            data = $.parseJSON(data);
            if (data.code == 0) {
                // 改变按钮的可用性
                // 将按钮设置为不可用，不能再点了
                // 这里要注意一下，想attr(),val()这些方法都是简化写法，实际上就是.attribute和.value方法，没有差别
                $("#topBtn").attr("disabled", "disabled")
            } else {
                alert(data.msg);
            }
        }
    );
}

// 加精
function setWonderful() {
    $.post(
        CONTEXT_PATH + "/discuss/wonderful",
        // 取得隐藏标签中的postId
        {"id":$("#postId").val()},
        // 回调函数 处理服务器响应结果
        function (data) {
            // 将json转成js对象
            data = $.parseJSON(data);
            if (data.code == 0) {
                // 改变按钮的可用性
                // 将按钮设置为不可用，不能再点了
                // 这里要注意一下，想attr(),val()这些方法都是简化写法，实际上就是.attribute和.value方法，没有差别
                $("#wonderfulBtn").attr("disabled", "disabled")
            } else {
                alert(data.msg);
            }
        }
    );
}

// 删除
function setDelete() {
    $.post(
        CONTEXT_PATH + "/discuss/delete",
        // 取得隐藏标签中的postId
        {"id":$("#postId").val()},
        // 回调函数 处理服务器响应结果
        function (data) {
            // 将json转成js对象
            data = $.parseJSON(data);
            if (data.code == 0) {
                // 删除帖子之后直接跳转回主页就可以了
                location.href = CONTEXT_PATH + "/index";
            } else {
                alert(data.msg);
            }
        }
    );
}