<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
</head>
<body>
<h2>文件上传</h2>
<form action="/manage/product/upload.do" method="post" enctype="multipart/form-data" name="upload">
    <input type="file" name="file"/>
    <input type="submit" value="上传"/>
</form>
<h2>富文本文件上传</h2>
<form action="/manage/product/richtext_image_upload.do" method="post" enctype="multipart/form-data" name="upload1">
    <input type="file" name="richtext_file"/>
    <input type="submit" value="上传"/>
</form>
</body>
</html>
