Dropzone.options.profileImage = {
    paramName: "file", // The name that will be used to transfer the file
    maxFilesize: 16, // MB --> Results in 240 GB for 15.000 users
    resizeWidth: 400,
    resizeHeight: 400,
    resizeQuality: 1,
    resizeMethod: 'crop',
    filesizeBase: 1024,
    acceptedFiles: "image/*",
    addRemoveLinks: false,
    capture: "camera",
    createImageThumbnails: false,
    previewsContainer: ".dropzone-hidden-previews",
    // previewTemplate: document.getElementById("dropzone-previews").innerHTML,
    // previewsContainer: "#dropzone-previews"
    init: function() {
        this.on("success", function(file, serverResponse) {
            // Called after the file successfully uploaded.

            // If the image is already a thumbnail:
            this.emit('thumbnail', file, serverResponse.url);

            // If it needs resizing:
            this.createThumbnailFromUrl(file, serverResponse.url);

            $(".profile-image").attr("src", serverResponse.url);
        });
        this.on("error", function(file, serverResponse) {
            // console.log(serverResponse.msg);
        });
    }
};