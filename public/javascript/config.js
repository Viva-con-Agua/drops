Dropzone.options.profileImage = {
    paramName: "file", // The name that will be used to transfer the file
    maxFilesize: 16, // MB --> Results in 240 GB for 15.000 users
    resizeWidth: 400,
    resizeHeight: 400,
    resizeQuality: 1,
    filesizeBase: 1024,
    acceptedFiles: "image/*",
    addRemoveLinks: true,
    capture: "camera"
};