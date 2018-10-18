window.markdownitImage = function(md, options) {
    md.renderer.rules.image = function(tokens, idx, options, env, self) {
        var token = tokens[idx];
        var srcIndex = token.attrIndex('src');
        var url = token.attrs[srcIndex][1];
        var title = '';
        var caption = token.content;

        if (token.attrIndex('title') !== -1) {
            title = ' title="' + token.attrs[token.attrIndex('title')][1] + '"'
        }

        return '<a class="image-link" href="#" onclick="return imageClick(\'' + url + '\')" >' +
                '<img src="' + url + '" alt="' + caption + '" ' + title + '>' +
            '</a>';
    }
};

function imageClick(url) {
    if (typeof JSInterface !== undefined) {
        JSInterface.image(url);
    }
    return false;
}