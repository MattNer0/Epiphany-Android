var forEach = function(array, callback, scope) {
    for (var i = 0; i < array.length; i++) {
        callback.call(scope, i, array[i]);
    }
};

hljs.configure({
    tabReplace: "<span class=\"hljs-tab\">    </span>"
});

var md = window.markdownit({
    html: true,
    breaks: true,
    linkify: true,
    typographer: true,
    highlight(str, lang) {
        if (lang && hljs.getLanguage(lang)) {
            try {
                return '<pre class="hljs"><code>' + hljs.fixMarkup(hljs.highlight(lang, str, true).value) + '</code></pre>';
            } catch (__) {}
        }
        return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>';
    }
});

md.use(window.markdownitFootnote);
md.use(window.markdownitCheckbox, {
    disabled: true,
    sourceMap: true,
    divWrap: true,
    divClass: 'check',
    liClass: 'checkbox',
    liClassChecked: 'checkbox-checked'
});

md.use(window.markdownitImage);
md.use(window.markdownitHeadinganchor, { anchorClass: 'epiphany-heading' });

var note_text = "";
var code_scroll = true;

function clickCheckbox(index, el) {
    el.onclick = function(event) {
        event.preventDefault();
        if (event.target.tagName == 'A') return;
        var i = 0;
        var checked;
        var ok = note_text.replace(/[*-]\s*(\[[x ]\])/g, function(x) {
            x = x.replace(/\s/g, ' ');
            var start = x.charAt(0);
            if (i == index) {
                i++;
                if (x == start + ' [x]') {
                    checked = false;
                    return start + ' [ ]';
                }
                checked = true;
                return start + ' [x]';
            }
            i++;
            return x;
        });
        note_text = ok;
        if (typeof JSInterface !== undefined) {
            JSInterface.checkbox(note_text, index, checked);
        }
        preview(note_text, code_scroll);
    };
}

function preview(md_text, codeScrollDisable) {
    if (md_text == "") return false;

    md_text = md_text.replace(/\\n/g, "\n");
    note_text = md_text;
    code_scroll = codeScrollDisable;

    var md_html = md.render(note_text);
    /*if (codeScrollDisable) {
        md_html = marked(md_text);
    }else{
        md_html = marked(md_text, {renderer: rend});
    }*/

    if (typeof JSInterface !== undefined) {
        JSInterface.body(md_html);
    }

    document.getElementById('preview').innerHTML = md_html;

    forEach(document.querySelectorAll('li.checkbox'), (index, el) => {
        clickCheckbox(index, el);
    });
}

function resizefont(font_size) {
    var html = document.getElementsByTagName("HTML")[0];
    html.className = "font" + font_size;
}

function settheme(theme) {
    var body = document.getElementsByTagName("BODY")[0];
    body.classList.add("theme-"+theme);
}