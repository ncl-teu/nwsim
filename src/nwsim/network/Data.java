package nwsim.network;

import java.io.Serializable;

/**
 * Created by Hidehiro Kanemitsu on 2021/10/22
 * データそのものを表すクラスです．
 */
public class Data implements Serializable {

    /**
     * コンテンツ
     */
    protected String content;

    /**
     * メタデータ
     */
    protected  String metaData;

    /**
     * データサイズ(KB)
     */
    protected long size;


    public Data(String content, String metaData, long size) {
        this.content = content;
        this.metaData = metaData;
        this.size = size;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
