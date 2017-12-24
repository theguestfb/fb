function toggleFormatting() {
    var x = document.getElementById("formattingDiv");
    var frame = document.getElementById("formattingFrame");
    var button = document.getElementById("formatButton");
    if (x.style.display === "none") {
        x.style.display = "block";
        button.value = "Hide Formatting Help";
    } else {
        x.style.display = "none";
        button.value = "Show Formatting Help";
    }
    frame.style.height = frame.contentWindow.document.body.scrollHeight + 'px';
}
