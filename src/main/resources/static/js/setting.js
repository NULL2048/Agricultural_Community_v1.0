// 表示页面加载完之后调用这个function方法
$(function () {
    // 表示点击了uploadForm这个form表单的submit按钮之后先触发upload这个方法
    $("#uploadForm").submit(upload);
})

function upload() {
    // 生成上传凭证
    var client = new OSS.Wrapper({
        // OSS所在区域
        region : 'oss-cn-beijing',
        accessKeyId : 'LTAI4Fv3hoH1iLTT2zQ2em5H',
        accessKeySecret : 'GpU9U3IVaT7SZXimMXotayqmfcp7g3',
        // 存储对象名
        bucket : 'acommunity-header'
    });

    // 得到界面中file的input标签的js对象,取得里面的文件流
    var file = document.getElementById("head-image").files[0];
    // 获取文件后缀
    var val = document.getElementById("head-image").value;
    var suffix = val.substr(val.indexOf("."));

    // 从界面中获取文件名称
    var fileName = $("#fileName").val();

    // 生成上传文件在服务器中的路径   就是bucket名/文件名
    var storeAs = "acommunity-header/" + fileName + suffix;

    // 打印日志
    console.log(file.name + ' => ' + storeAs);

    // 上传文件  then方法是处理服务器的响应回来数据
    client.multipartUpload(storeAs, file).then(function (result) {
        // 如果上传成功则更改mysql数据库中的headerUrl
            // 更新头像访问路径
            $.post(
                CONTEXT_PATH + "/user/header/url",
                // 注意这里需要传入文件名和后缀名，否则到时候根据url访问不到图片
                {"fileName":fileName + suffix},
                function (data) {
                    data = $.parseJSON(data);
                    // 修改成功则刷新页面
                    if (data.code == 0) {
                        window.location.reload();
                    } else {
                        alert(data.msg);
                    }
                }
            );
    }).catch(function (err) { // 如果在上面这个大括号中的任何代码出现了异常，都由下面这个catch中的方法来处理异常
        alert("上传失败！");
        console.log(err);
    });

    // 这里一定要返回false，如果不返回false，这个form表单还是通过页面提交一次，页面的form没有写action，就会出现错误
    return false;
}