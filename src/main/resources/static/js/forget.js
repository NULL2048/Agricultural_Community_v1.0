// 下面这个方法就一进到这个页面就开始执行
$(function(){
    //$("#codeBtn").text("获取验证码");
    // 对发布按钮做一个点击事件，点了发布按钮之后就调用publish()方法  有一点需要特别注意这里的括号只写方法名，不能在后面加括号，因为js语法中加了括号就表示执行方法，所以如果这里加了括号不管点不点击都会执行这个方法
    $("#codeBtn").click(sendCode);
});

function sendCode() {
    // 获取邮箱地址
    var email = $("#your-email").val();
    // 判断邮箱格式
    var reg = /^\w+((-\w+)|(\.\w+))*\@[A-Za-z0-9]+((\.|-)[A-Za-z0-9]+)*\.[A-Za-z0-9]+$/;
    isok= reg.test(email);
    if (!isok) {
        alert("发送失败!");
    }

    // 发送异步请求（POST）相关笔记在HTML文件夹的demo页面里面
    $.post(
        CONTEXT_PATH + "/code",
        {"email":email},
        // 添加回调函数，处理返回结果
        function (data) {
            data = $.parseJSON(data);

            if (data.code == 0) {
                $("#codeBtn").text("已发送验证码");

                $('#codeBtn').attr("disabled",false);
            }
        }
    );

}