package com.bumptech.glide.gifdecoder;

import com.bumptech.glide.gifdecoder.test.GifBytesTestUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link com.bumptech.glide.gifdecoder.GifHeaderParser}.
 */
public class GifHeaderParserTest {
    private GifHeaderParser parser;

    @Before
    public void setUp() {
        parser = new GifHeaderParser();
    }

    @Test
    public void testReturnsHeaderWithFormatErrorIfDoesNotStartWithGifHeader() {
        parser.setData("wrong_header".getBytes());
        GifHeader result = parser.parseHeader();
        assertEquals(GifDecoder.STATUS_FORMAT_ERROR, result.status);
    }

    @Test
    public void testCanReadValidHeaderAndLSD() {
        final int width = 10;
        final int height = 20;
        ByteBuffer buffer = ByteBuffer.allocate(GifBytesTestUtil.HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        GifBytesTestUtil.writeHeaderAndLsd(buffer, width, height, false, 0);

        parser.setData(buffer.array());
        GifHeader header = parser.parseHeader();
        assertEquals(width, header.width);
        assertEquals(height, header.height);
        assertFalse(header.gctFlag);
        // 2^(1+0) == 2^1 == 2.
        assertEquals(2, header.gctSize);
        assertEquals(0, header.bgIndex);
        assertEquals(0, header.pixelAspect);
    }

    @Test
    public void testCanParseHeaderOfTestImageWithoutGraphicalExtension() throws IOException {
        byte[] data = TestUtil.readResourceData("gif_without_graphical_control_extension.gif");
        parser.setData(data);
        GifHeader header = parser.parseHeader();
        assertEquals(1, header.frameCount);
        assertNotNull(header.frames.get(0));
        assertEquals(GifDecoder.STATUS_OK, header.status);
    }

    @Test
    public void testCanReadImageDescriptorWithoutGraphicalExtension() {
        final int lzwMinCodeSize = 2;
        ByteBuffer buffer = ByteBuffer.allocate(
                GifBytesTestUtil.HEADER_LENGTH
                + GifBytesTestUtil.IMAGE_DESCRIPTOR_LENGTH
                + GifBytesTestUtil.getImageDataSize(lzwMinCodeSize)
        ).order(ByteOrder.LITTLE_ENDIAN);
        GifBytesTestUtil.writeHeaderAndLsd(buffer, 1, 1, false, 0);
        GifBytesTestUtil.writeImageDescriptor(buffer, 0, 0, 1, 1, false /*hasLct*/, 0);
        GifBytesTestUtil.writeFakeImageData(buffer, lzwMinCodeSize);

        parser.setData(buffer.array());
        GifHeader header = parser.parseHeader();
        assertEquals(1, header.width);
        assertEquals(1, header.height);
        assertEquals(1, header.frameCount);
        assertNotNull(header.frames.get(0));
    }

    @Test
    public void testSetsFrameLocalColorTableToNullIfNoColorTable() {
        final int lzwMinCodeSize = 2;
        ByteBuffer buffer = ByteBuffer.allocate(
                GifBytesTestUtil.HEADER_LENGTH
                + GifBytesTestUtil.IMAGE_DESCRIPTOR_LENGTH
                + GifBytesTestUtil.getImageDataSize(lzwMinCodeSize)
        ).order(ByteOrder.LITTLE_ENDIAN);
        GifBytesTestUtil.writeHeaderAndLsd(buffer, 1, 1, false, 0);
        GifBytesTestUtil.writeImageDescriptor(buffer, 0, 0, 1, 1, false /*hasLct*/, 0);
        GifBytesTestUtil.writeFakeImageData(buffer, lzwMinCodeSize);

        parser.setData(buffer.array());
        GifHeader header = parser.parseHeader();
        assertEquals(1, header.width);
        assertEquals(1, header.height);
        assertEquals(1, header.frameCount);
        assertNotNull(header.frames.get(0));
        assertNull(header.frames.get(0).lct);
    }

    @Test
    public void testSetsFrameLocalColorTableIfHasColorTable() {
        final int lzwMinCodeSize = 2;
        final int numColors = 4;
        ByteBuffer buffer = ByteBuffer.allocate(
                GifBytesTestUtil.HEADER_LENGTH
                + GifBytesTestUtil.IMAGE_DESCRIPTOR_LENGTH
                + GifBytesTestUtil.getImageDataSize(lzwMinCodeSize)
                + GifBytesTestUtil.getColorTableLength(numColors)
        ).order(ByteOrder.LITTLE_ENDIAN);
        GifBytesTestUtil.writeHeaderAndLsd(buffer, 1, 1, false, 0);
        GifBytesTestUtil.writeImageDescriptor(buffer, 0, 0, 1, 1, true /*hasLct*/, numColors);
        GifBytesTestUtil.writeColorTable(buffer, numColors);
        GifBytesTestUtil.writeFakeImageData(buffer, 2);

        parser.setData(buffer.array());
        GifHeader header = parser.parseHeader();
        assertEquals(1, header.width);
        assertEquals(1, header.height);
        assertEquals(1, header.frameCount);
        assertNotNull(header.frames.get(0));

        GifFrame frame = header.frames.get(0);
        assertNotNull(frame.lct);
    }

    @Test
    public void testCanParseMultipleFrames() {
        final int lzwMinCodeSize = 2;
        final int expectedFrames = 3;

        final int frameSize = GifBytesTestUtil.IMAGE_DESCRIPTOR_LENGTH
                + GifBytesTestUtil.getImageDataSize(lzwMinCodeSize);
        ByteBuffer buffer = ByteBuffer.allocate(
                GifBytesTestUtil.HEADER_LENGTH + expectedFrames * frameSize
        ).order(ByteOrder.LITTLE_ENDIAN);

        GifBytesTestUtil.writeHeaderAndLsd(buffer, 1, 1, false, 0);
        for (int i = 0; i < expectedFrames; i++) {
            GifBytesTestUtil.writeImageDescriptor(buffer, 0, 0, 1, 1, false /*hasLct*/, 0 /*numColors*/);
            GifBytesTestUtil.writeFakeImageData(buffer, 2);
        }

        parser.setData(buffer.array());
        GifHeader header = parser.parseHeader();
        assertEquals(expectedFrames, header.frameCount);
        assertEquals(expectedFrames, header.frames.size());
    }

    @Test(expected = IllegalStateException.class)
    public void testThrowsIfParseHeaderCalledBeforeSetData() {
        GifHeaderParser parser = new GifHeaderParser();
        parser.parseHeader();
    }
}