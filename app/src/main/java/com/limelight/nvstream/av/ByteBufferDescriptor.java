package com.limelight.nvstream.av;

public class ByteBufferDescriptor {
    public byte[] data;
    public int offset;
    public int length;
    
    public ByteBufferDescriptor nextDescriptor;
    
    public ByteBufferDescriptor(byte[] data, int offset, int length)
    {
        this.data = data;
        this.offset = offset;
        this.length = length;
    }
    
    public ByteBufferDescriptor(ByteBufferDescriptor desc)
    {
        this.data = desc.data;
        this.offset = desc.offset;
        this.length = desc.length;
    }
    
    public void reinitialize(byte[] data, int offset, int length)
    {
        this.data = data;
        this.offset = offset;
        this.length = length;
        this.nextDescriptor = null;
    }
    
}
