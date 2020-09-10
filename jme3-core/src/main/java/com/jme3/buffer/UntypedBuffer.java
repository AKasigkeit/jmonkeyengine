/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.buffer;

import com.jme3.renderer.Renderer;
import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GL3;
import com.jme3.renderer.opengl.GL4;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import com.jme3.util.NativeObject;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 *
 * @author Alexander Kasigkeit
 */
public class UntypedBuffer extends NativeObject {

    public static enum MemoryMode {
        /**
         * Buffer is set on the CPU, send to the GPU and kept at both places
         */
        CpuGpu,
        /**
         * Only a size is specified, the buffer is created on the GPU and can be
         * updated in chunks of filled via ComputeShaders etc
         */
        GpuOnly
    }

    public static enum StorageApi {
        /**
         * Allows for the usage of StorageFlags and persistent mapping. Changing
         * the buffer size will result in creating a completly new buffer on the
         * gpu. Might result in better performance in some cases because the
         * storage specifications are immutable whereas with BufferData those
         * might be changed by the user at any time. Persistent mapping is
         * useful for updating buffers on the GPU each frame, however you need
         * to make sure to flush / memoryBarrier your writes to ensure
         * visibility <b>Requires OpenGL 4.4</b>
         */
        Storage,
        /**
         * No specific OpenGL requirements. Mapping the buffer is still allowed
         * (that however requires OpenGL 3.0), however persistent / coherent
         * mapping is only allowed for StorageApi.Storage
         */
        BufferData
    }

    /**
     * Those are only hints. The behaviour differs from implementation to
     * implementation but on average those will probably end up with the same
     * performance since the driver is free to switch from one layout to another
     * without the user noticing it when it sees fit
     */
    public static enum BufferDataUsage {
        /**
         * Use this if the data occasionally changes and will only be used for
         * rendering
         */
        DynamicDraw(GL.GL_DYNAMIC_DRAW),
        /**
         * Use this if the data occasionally changes and the buffer will be used
         * as destination for gl commands like transform feedback
         */
        DynamicRead(GL.GL_DYNAMIC_READ),
        /**
         * Use this if the data occasionally changes and the buffer will be used
         * as intermediate buffer without directly writing data to it
         */
        DynamicCopy(GL.GL_DYNAMIC_COPY),
        /**
         * Use this if the data is set once and used many times for rendering
         */
        StaticDraw(GL.GL_STATIC_DRAW),
        /**
         * Use this if the data is set once and afterwards the buffer is used as
         * terget for reads
         */
        StaticRead(GL.GL_STATIC_READ),
        /**
         * Use this is the data is set once and afterwards the buffer is used as
         * target for copies
         */
        StaticCopy(GL.GL_STATIC_COPY),
        /**
         * Use this if the data is used for rendering about as often as it is
         * changed
         */
        StreamDraw(GL.GL_STREAM_DRAW),
        /**
         * Use this if the data is used for reading as often as data is written
         * to it, doesnt make sence? right! if you want actual control, use
         * StorageApi.Storage
         */
        StreamRead(GL.GL_STREAM_READ),
        /**
         * Use this if the data is used for copying as often as data is written
         * to it
         */
        StreamCopy(GL.GL_STREAM_COPY);

        private final int GL_CONST;

        private BufferDataUsage(int usage) {
            GL_CONST = usage;
        }

        public int getGlConstant() {
            return GL_CONST;
        }
    }

    public static enum StorageFlag {
        /**
         * No Storage Flags. Created buffer will ne immutable in terms of
         * storage and data.
         */
        None(0),
        /**
         * Use this to indicate that the contents of the buffer might change
         * after initialization.
         */
        Dynamic(GL4.GL_DYNAMIC_STORAGE_BIT),
        /**
         * Use this to indicate that the buffer might be mapped for read access.
         */
        MapRead(GL4.GL_MAP_READ_BIT),
        /**
         * Use this to indicate that the buffer might be mapped for write
         * access.
         */
        MapWrite(GL4.GL_MAP_WRITE_BIT),
        /**
         * Use this to indicate that the buffer might be used to render/compute
         * while it is still mapped. If this flag is used, at least one of Read
         * or Write MUST also be used.
         */
        MapPersistent(GL4.GL_MAP_PERSISTENT_BIT),
        /**
         * Use this to make sure subsequent calls will see values written to the
         * buffer if the buffer is mapped. If this flags is used, Persistent
         * flag MUST also be used.
         */
        MapCoherent(GL4.GL_MAP_READ_BIT);

        private final int GL_CONST;

        private StorageFlag(int val) {
            GL_CONST = val;
        }

        public int getGlConstant() {
            return GL_CONST;
        }

        public static int fromArray(StorageFlag[] flags) {
            if (flags == null) {
                return 0;
            }
            int fl = 0;
            for (StorageFlag f : flags) {
                fl |= f.GL_CONST;
            }
            return fl;
        }
    }

    public static enum MappingFlag {
        /**
         * Use this to indicate that the buffer might be mapped for read access.
         */
        Read(GL4.GL_MAP_READ_BIT),
        /**
         * Use this to indicate that the buffer might be mapped for write
         * access.
         */
        Write(GL4.GL_MAP_WRITE_BIT),
        /**
         * Use this to indicate that the buffer might be used to render/compute
         * while it is still mapped. If this flag is used, at least one of Read
         * or Write MUST also be used.
         */
        Persistent(GL4.GL_MAP_PERSISTENT_BIT),
        /**
         * Use this to make sure subsequent calls will see values written to the
         * buffer if the buffer is mapped. If this flags is used, Persistent
         * flag MUST also be used. If this flag is not used,
         * renderer.memoryBarrier() must be called with the
         * MemoryBarrierBit.CLIENT_MAPPED_BUFFER bit set
         */
        Coherent(GL4.GL_MAP_READ_BIT),
        /**
         * Use this to indicate that previous content of the buffer when mapping
         * a range can be discarded. Can not be used together with MapRead
         */
        InvalidateRange(GL3.GL_MAP_INVALIDATE_RANGE_BIT),
        /**
         * Use this to indicate that the previous content of the whole bffer can
         * be discarded even when mapping only a range.
         */
        Invalidate(GL3.GL_MAP_INVALIDATE_BUFFER_BIT),
        /**
         * Use this to indicate that you will manually flush the contents. The
         * contents are flushed automatically when unmapping the buffer
         */
        ExplicitFlush(GL3.GL_MAP_FLUSH_EXPLICIT_BIT),
        /**
         * Use this to indicate that GL should not try to synchronize any
         * pending openartions on this buffer. Notice that the behaviour is
         * undefined when modifying content that is is use
         */
        Unsynchronized(GL3.GL_MAP_UNSYNCHRONIZED_BIT);

        private final int GL_CONST;

        private MappingFlag(int val) {
            GL_CONST = val;
        }

        public int getGlConstant() {
            return GL_CONST;
        }

        public static int fromArray(MappingFlag[] flags) {
            if (flags == null) {
                return 0;
            }
            int fl = 0;
            for (MappingFlag f : flags) {
                fl |= f.GL_CONST;
            }
            return fl;
        }
    }

    /**
     * Creates a new UntypedBuffer instance using the Storage Api (which
     * requires Opengl 3.0) but allows for persistent mapping (which requires
     * OpenGL 4.4) and will run in direct mode, ie data will be sent to GL the
     * moment the updateData() method is called, which allows for efficient
     * uploading of data in chunks on a loop.
     *
     * @param mode MemoryMode to use (of of MemoryMode.CpuGpu or
     * MemoryMode.GpuOnly)
     * @param renderer renderer needed to run in direct mode
     * @param flags flags the indicate the usage of the buffer
     * @return the new UntypedBuffer instance
     */
    public static UntypedBuffer createNewStorageDirect(MemoryMode mode, Renderer renderer, StorageFlag... flags) {
        if (renderer == null) {
            throw new IllegalArgumentException("renderer cannot be null for UntypedBuffer in direct mode");
        }
        return new UntypedBuffer(mode, renderer, null, flags);
    }

    /**
     * Creates a new UntypedBuffer instance using the Storage Api (which
     * requires Opengl 3.0) but allows for persistent mapping (which requires
     * OpenGL 4.4) and it will run in lazy mode, ie data will first be sent to
     * the GPU when it is needed.
     *
     * @param mode MemoryMode to use (of of MemoryMode.CpuGpu or
     * MemoryMode.GpuOnly)
     * @param flags flags the indicate the usage of the buffer
     * @return the new UntypedBuffer instance
     */
    public static UntypedBuffer createNewStorageLazy(MemoryMode mode, StorageFlag... flags) {
        return new UntypedBuffer(mode, null, null, flags);
    }

    /**
     * Creates a new UntypedBuffer instance using the BufferData Api (which does
     * NOT allow for persistent mapping, but is available on all platforms) and
     * will run in direct mode, ie data will be sent to GL the moment the
     * updateData() method is called, which allows for efficient uploading of
     * data in chunks on a loop.
     *
     * @param mode MemoryMode to use (of of MemoryMode.CpuGpu or
     * MemoryMode.GpuOnly)
     * @param renderer renderer needed to run in direct mode
     * @param usage usage that indicates the usage of this buffer
     * @return the new UntypedBuffer instance
     */
    public static UntypedBuffer createNewBufferDataDirect(MemoryMode mode, Renderer renderer, BufferDataUsage usage) {
        if (renderer == null) {
            throw new IllegalArgumentException("renderer cannot be null for UntypedBuffer in direct mode");
        }
        return new UntypedBuffer(mode, renderer, usage);
    }

    /**
     * Creates a new UntypedBuffer instance using the BufferData Api (which does
     * NOT allow for persistent mapping, but is available on all platforms) and
     * it will run in lazy mode, ie data will first be sent to the GPU once it
     * is needed
     *
     * @param mode MemoryMode to use (of of MemoryMode.CpuGpu or
     * MemoryMode.GpuOnly)
     * @param usage usage that indicates the usage of this buffer
     * @return the new UntypedBuffer instance
     */
    public static UntypedBuffer createNewBufferDataLazy(MemoryMode mode, BufferDataUsage usage) {
        return new UntypedBuffer(mode, null, usage);
    }

    private final MemoryMode MEM_MODE;
    private final Renderer RENDERER;
    private final int BUFFERDATA_USAGE;
    private final int STORAGE_FLAGS;

    private ByteBuffer cpuData = null;
    private int gpuSize = 0;
    private boolean initialized = false;

    private BufferMappingHandle mappingHandle = null;
    //size changes
    private boolean sizeChanged = false;
    private int previousGpuSize = -1;
    //updates
    private int updateOffset = -1;
    private int updatePosition = -1;
    private int updateSize = -1;
    private ByteBuffer updateBuffer = null;
    //downloads
    private int downloadOffset = -1;
    private int downloadPosition = -1;
    private int downloadSize = -1;
    private ByteBuffer downloadBuffer = null;

    private UntypedBuffer(MemoryMode mode, Renderer renderer, BufferDataUsage bdUsage, StorageFlag... stFlags) {
        if (mode == null) {
            throw new IllegalArgumentException("MemoryMode cannot be null");
        }
        if (bdUsage != null && (stFlags != null && stFlags.length > 0)) {
            throw new IllegalArgumentException("Can only specify BufferDataUsage or StorageFlags but not both");
        } else if (bdUsage == null && stFlags == null) {
            throw new IllegalArgumentException("Need to specify either BufferDataUsage or StorageFlags");
        }

        MEM_MODE = mode;
        RENDERER = renderer;
        BUFFERDATA_USAGE = bdUsage == null ? -1 : bdUsage.getGlConstant();
        STORAGE_FLAGS = StorageFlag.fromArray(stFlags);
    }

    //constructor for destructable copy
    private UntypedBuffer(int id, MemoryMode mode, int bufferdataUsage, int storageFlags, Renderer renderer) {
        super(id);
        MEM_MODE = mode;
        BUFFERDATA_USAGE = bufferdataUsage;
        STORAGE_FLAGS = storageFlags;
        RENDERER = renderer;
    }

    /**
     * Initialization for GPU-Only buffers.
     *
     * @param size the initial size of the buffer in bytes
     * @return this for convenience
     */
    public UntypedBuffer initialize(int size) {
        if (MEM_MODE != MemoryMode.GpuOnly) {
            throw new UnsupportedOperationException("This initialize() method can only be used with Gpu-Only buffers.");
        } else if (initialized) {
            throw new UnsupportedOperationException("This buffer is already initialized.");
        } else if (size < 1) {
            throw new IllegalArgumentException("size cannot be smaller than 1 byte");
        }
        gpuSize = size;
        if (isDirect()) {
            RENDERER.updateBuffer(this);
        }
        initialized = true;
        return this;
    }

    /**
     * Initialization for Mode.CpuGpu buffers. The whole Buffer will be used, no
     * matter its current position or limit as oppotes to the updateData()
     * method, which will only use the data between buffer.position and
     * buffer.limit
     *
     * @param data the data to use for this buffer
     * @return this same instance for convenience
     */
    public UntypedBuffer initialize(ByteBuffer data) {
        if (MEM_MODE != MemoryMode.CpuGpu) {
            throw new UnsupportedOperationException("This initialize() method can only be used with CpuGpu-buffers.");
        } else if (initialized) {
            throw new UnsupportedOperationException("This buffer is already initialized.");
        } else if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }
        cpuData = data;
        gpuSize = cpuData.capacity();

        if (isDirect()) {
            RENDERER.updateBuffer(this);
        }
        initialized = true;
        return this;
    }

    /**
     * Changes this buffer's size on the GPU, can only be used with GPU-Only
     * buffers but regardless of the StorageApi. Any old content will be copied,
     * the content of the new area in case the new size is bigger is undefined.
     * In case the specified size is smaller, the values at the end will be cut
     * off.
     *
     * @param bytes new size in bytes
     */
    public void changeSize(int bytes) {
        if (!initialized) {
            throw new UnsupportedOperationException("Can only change the size after this buffer has been initialized");
        }
        if (MEM_MODE != MemoryMode.GpuOnly) {
            throw new UnsupportedOperationException("This method can only be used with Gpu-Only buffers.");
        }

        previousGpuSize = gpuSize;
        gpuSize = bytes;
        if (id != -1) {
            sizeChanged = true;
        }

        if (isDirect()) {
            RENDERER.updateBuffer(this);
        }
    }

    /**
     * Does not work with GPU-Only buffers. When changes have been made directly
     * to the underlying CPU-buffer, those changed regions can be marked here so
     * they get properly send to the GPU buffer as well. It is recommended to
     * only use this method with UntypedBuffers in direct mode, otherwise only
     * one update per frame will be visible (one update per usage in rendering
     * actually)
     *
     * @param offset offset into the buffer in bytes where changes have been
     * made
     * @param length size of the block that was changed in bytes
     */
    public void markUpdate(int offset, int length) {
        if (!initialized) {
            throw new UnsupportedOperationException("cannot update data when this buffer has not yet been initialized");
        }
        if (MEM_MODE == MemoryMode.GpuOnly) {
            throw new UnsupportedOperationException("cannot mark update of buffer that is GPU only");
        }
        if (offset + length > cpuData.capacity()) {
            throw new IllegalArgumentException("specified offset + length exceed current CPU buffers capacity");
        }

        updateOffset = offset;
        updatePosition = offset;
        updateSize = length;
        if (isDirect()) {
            RENDERER.updateBuffer(this);
        }
    }

    /**
     * Updates the data of the buffer. the actual data is taken from
     * buffer.position() until buffer.limit() and applied to the current buffer
     * starting at the specified offset. Changes size on cpu and gpu
     * automatically if the update writes behind the current boudaries. Any
     * previous contents are kept in this case.
     *
     * @param data the buffer holding the data for update (between position and
     * limit)
     * @param offset offset into this buffer to start writing the update
     */
    public void updateData(ByteBuffer data, int offset) {
        if (!initialized) {
            throw new UnsupportedOperationException("cannot update data when this buffer has not yet been initialized");
        }
 
        updateOffset = offset;
        updatePosition = data.position();
        updateSize = data.remaining();
        if (MEM_MODE == MemoryMode.GpuOnly) {
            int neededSize = offset + updateSize;
            if (neededSize > gpuSize) {
                if (id != -1) {
                    previousGpuSize = gpuSize; //TODO check if we only need to set it if previousGpuSize is -1
                    sizeChanged = true;
                }
                gpuSize = neededSize;
            }
            updateBuffer = data; //since theres no cpu data buffer, remember this one (if in lazy mode, it will be overridden)
        } else {
            if (offset + updateSize > cpuData.capacity()) { //grow 
                ByteBuffer newBuffer = BufferUtils.createByteBuffer(Math.max(offset + updateSize, data.capacity())); 
                for (int i = 0; i < Math.min(offset, cpuData.capacity()); i++) {
                    newBuffer.put(i, cpuData.get(i));
                }
                BufferUtils.destroyDirectBuffer(cpuData); //destroy old
                cpuData = newBuffer;
                if (id != -1) {
                    sizeChanged = true; //if the buffer was created already, mark size change
                    previousGpuSize = gpuSize;
                }
                gpuSize = cpuData.capacity();
            }
            //put update into buffer
            for (int i = 0; i < updateSize; i++) {
                cpuData.put(offset + i, data.get(i + data.position()));
            }
        }

        //if its direct, initialization already created it thus this is an actual update
        //however in lazy mode, just because initialize() was called, doesnt mean the buffer was created 
        if (isDirect()) {
            RENDERER.updateBuffer(this);
        } else if (id == -1) {
            clearPendingUpdate();
        }
    }

    /**
     * Retrieves the data from this UntypedBuffer starting at the specified
     * offset and fills the area between the specified store's current position
     * and its current limit.The content is downloaded from the Gpu, however
     * only in direct mode the buffer will be filled with data right away,
     * UntypedBuffers in lazy mode will have the download pending until the
     * buffer is used by the renderer next time in which case requesting another
     * download will override the current request
     *
     * @param store the ByteBuffer to store the data in (between its current
     * position and limit)
     * @param offset the offset into this UntypedBuffer to start reading values
     * from
     */
    public void downloadData(ByteBuffer store, int offset) {
        if (MEM_MODE != MemoryMode.GpuOnly) {
            throw new UnsupportedOperationException("This method can only be used with GPU-Only buffers");
        }

        downloadBuffer = store;
        downloadOffset = offset;
        downloadSize = store.limit() - store.position();
        downloadPosition = store.position();
        if (isDirect()) {
            RENDERER.updateBuffer(this);
        }
    }

    public void downloadData(int offset, int length) {
        if (MEM_MODE != MemoryMode.CpuGpu) {
            throw new UnsupportedOperationException("This method can only be used with CpuGpu buffers");
        }

        downloadBuffer = cpuData;
        downloadOffset = offset;
        downloadSize = length;
        downloadPosition = offset;
        if (isDirect()) {
            RENDERER.updateBuffer(this);
        }
    }

    /**
     * Returns true if this buffers size was change and still needs to be
     * changed accordingly on the GPU
     *
     * @return true if this buffer has a size change pending for the GPU buffer
     */
    public boolean hasPendingSizeChange() {
        return sizeChanged;
    }

    /**
     * In case this buffers size was changed and the GPU didnt yet pick up the
     * change, returns the size the buffer currently has on the GPU and
     * previously had on the CPU also
     *
     * @return previus size of the buffer in case the size changed
     */
    public int getPendingSizeChangePreviousSize() {
        return previousGpuSize;
    }

    @Override
    public void clearUpdateNeeded() {
        super.clearUpdateNeeded();
        sizeChanged = false;
    }

    /**
     * Returns true if this buffer has any updates still to be uploaded to the
     * GPU
     *
     * @return true in case any updates for the GPU buffer are pending
     */
    public boolean hasPendingUpdate() {
        return updateOffset != -1;
    }

    /**
     * Returns the offset to start writing the update into the GPU buffer in
     * case any update is pending, or -1 otherwise
     *
     * @return
     */
    public int getPendingUpdateOffset() {
        return updateOffset;
    }

    /**
     * Returns the buffer that should be used for the upload to the GPU. In case
     * of CpuGpu buffers, this is the CPU-side of the buffer. In case of
     * GPU-Only buffers, this is the buffer that was set to update the data
     *
     * @return the buffer used for updating the GPU buffer
     */
    public ByteBuffer getPendingUpdateBuffer() {
        if (updateBuffer != null) { //if we got the update buffer, it has its data for the update stored at its position
            return updateBuffer;
        } else { //cpuBuffer might have the position anywhere, set it accordingly
            cpuData.position(updatePosition);
            cpuData.limit(updatePosition + updateSize);
            return cpuData;
        }
    }

    /**
     * INTERNAL USE ONLY. Clears any pending update flags to make sure same data
     * will not be uploaded more than once
     */
    public void clearPendingUpdate() {
        updateOffset = -1;
        updatePosition = -1;
        updateSize = -1;
        updateBuffer = null;
    }

    /**
     * Returns true if this buffer has any pending downloads (that is loading
     * data back from the GPU). Can only be true for GPU-Only buffers, because
     * CpuGpu-buffers will copy the contents from the CPU right away and never
     * have anything pending
     *
     * @return true if this buffer has any pending download
     */
    public boolean hasPendingDownload() {
        return downloadOffset != -1;
    }

    /**
     * Returns the offset into the buffer on the GPU to start downloading data
     * from, or -1 if no downloading is pending
     *
     * @return offset into GPU buffer so tart reading data
     */
    public int getPendingDownloadOffset() {
        return downloadOffset;
    }

    /**
     * Returns the buffer to store the data downloaded from the GPU in or null
     * if no download is pending
     *
     * @return buffer to store downloaded data in
     */
    public ByteBuffer getPendingDownloadBuffer() {
        return downloadBuffer;
    }

    /**
     * INTERNAL USE ONLY. Clears any pending downloads to make sure same data is
     * not downloaded more than once
     */
    public void clearPendingDownload() {
        downloadOffset = -1;
        downloadPosition = -1;
        downloadSize = -1;
        downloadBuffer = null;
    }

    /**
     * Returns true if and only if this buffer is in direct mode, false
     * otherwise
     *
     * @return tre in case this buffer is in direct mode
     */
    public boolean isDirect() {
        return RENDERER != null;
    }

    /**
     * Returns the MemoryMode of this UntypedBuffer, one of MemoryMode.CpuGpu or
     * MemoryMode.GpuOnly
     *
     * @return this buffers memory mode
     */
    public MemoryMode getMemoryMode() {
        return MEM_MODE;
    }

    /**
     * Returns the StorageAPI of this UntypedBuffer, one of StorageApi.Storage
     * or StorageApi.BufferData
     *
     * @return
     */
    public StorageApi getStorageAPI() {
        return BUFFERDATA_USAGE == -1 ? StorageApi.Storage : StorageApi.BufferData;
    }

    /**
     * Returns the underlying CPU-buffer, when changes have been made to this
     * buffer, make sure to call markUpdate() with appropriate offset and size
     * to ensure visibility on the GPU as well. Returns null for Gpu-Only
     * buffers
     *
     * @return this buffers underlying Cpu-buffer if the MemoryMode is CpuGpu,
     * null otherwise
     */
    public ByteBuffer getCpuData() {
        return cpuData;
    }

    /**
     * Returns the BufferDataUsage bits if this buffers StorageApi is BufferData
     * or -1 otherwise
     *
     * @return BufferDataUsage bits, or -1 if StorageApi is Storage
     */
    public int getBufferDataUsage() {
        return BUFFERDATA_USAGE;
    }

    /**
     * Returns the StorageFlag bits if this buffers StorageApi is Storage or -1
     * otherwise
     *
     * @return StorageFlag bits, or -1 if StorageApi is BufferData
     */
    public int getStorageFlags() {
        return STORAGE_FLAGS;
    }

    /**
     * Returns the size of the buffer on the Gpu (in direct mode, this number is
     * up-to-date, however in lazy mode this is the size the buffer will have
     * after the next usage of this buffer in rendering
     *
     * @return size of this buffer on the GPU in bytes
     */
    public int getSizeOnGpu() {
        return (int) gpuSize;
    }

    /**
     * Returns the current size of the buffer on the CPU or 0 if this buffer is
     * in MemoryMode GpuOnly
     *
     * @return size of this buffer on the CPU in bytes or 0 for GpuOnly buffers
     */
    public int getSizeOnCpu() {
        return cpuData != null ? cpuData.capacity() : 0;
    }

    /**
     * Returns true if and only if this UntypedBuffer is currently mapped, flase
     * otherwise
     *
     * @return true if this buffer is mapped
     */
    public boolean isMapped() {
        return mappingHandle != null;
    }

    /**
     * Only works with Gpu-Only buffer. Maps the entire buffer
     *
     * @param flags mapping flags to use
     * @return the BufferMappingHandle to read/write to the GL memory
     */
    public BufferMappingHandle mapBuffer(MappingFlag... flags) {
        return mapBufferRange(0, (int) gpuSize, flags);
    }

    /**
     * Only works with Gpu-Only buffers. Maps the specified range
     *
     * @param offset offset in bytes to start mapping
     * @param length length in bytes to map
     * @param flags MappingFlags that specify the type of mapping to establish
     * @return BufferMappingHandle to handle the mapping
     */
    public BufferMappingHandle mapBufferRange(int offset, int length, MappingFlag... flags) {
        if (mappingHandle != null) {
            throw new UnsupportedOperationException("this buffer is currently mapped already");
        }
        if (flags == null || flags.length == 0) {
            throw new IllegalArgumentException("need to specify mapping flags");
        }
        if (MEM_MODE != MemoryMode.GpuOnly) {
            throw new UnsupportedOperationException("mapping is only available for GPU-only buffers");
        }
        if (!isDirect()) {
            throw new UnsupportedOperationException("mapping is only available for buffers in direct mode");
        }
        if (getStorageAPI() != StorageApi.Storage) {
            throw new UnsupportedOperationException("mapping is only available for buffers using StorageApi.Storage");
        }
        if ((STORAGE_FLAGS & (StorageFlag.MapRead.getGlConstant() | StorageFlag.MapWrite.getGlConstant())) == 0) {
            throw new UnsupportedOperationException("mapping is only available for buffer that have at least MapWrite or MapRead StorageFlags set");
        }

        int flagBits = MappingFlag.fromArray(flags);
        boolean isPersistent = (flagBits & MappingFlag.Persistent.getGlConstant()) != 0;
        boolean isCoherent = (flagBits & MappingFlag.Coherent.getGlConstant()) != 0;
        boolean isRead = (flagBits & MappingFlag.Read.getGlConstant()) != 0;
        boolean isWrite = (flagBits & MappingFlag.Write.getGlConstant()) != 0;
        if (isPersistent && !(isRead || isWrite)) {
            throw new IllegalArgumentException("cannot set MappingFlag.Persistent without also setting MappingFlag.Read or MappingFlag.Write");
        }
        if (isCoherent && !isPersistent) {
            throw new IllegalArgumentException("cannot set MappingFlag.Coherent without also setting MappingFlag.Persistent");
        }
        if (isPersistent && ((STORAGE_FLAGS & StorageFlag.MapPersistent.getGlConstant()) == 0)) {
            throw new IllegalArgumentException("cannot set MappingFlag.Persistent without also setting StorageFlag.MapPersistent on creation");
        }
        if (isCoherent && ((STORAGE_FLAGS & StorageFlag.MapCoherent.getGlConstant()) == 0)) {
            throw new IllegalArgumentException("cannot set MappingFlag.Coherent without also setting StorageFlag.MapCoherent on creation");
        }

        mappingHandle = RENDERER.mapBuffer(this, offset, length, flags);
        return mappingHandle;
    }

    public static class BufferMappingHandle {

        private final int flags;
        private final UntypedBuffer untypedBuffer;
        private final ByteBuffer buffer;
        private final int offset, length;

        public BufferMappingHandle(UntypedBuffer untypedBuffer, ByteBuffer directBuffer, int offset, int length, int mappingFlags) {
            this.untypedBuffer = untypedBuffer;
            this.offset = offset;
            this.length = length;
            this.buffer = directBuffer;
            this.flags = mappingFlags;
        }

        /**
         * Only works with buffers that have been mapped using the
         * MappingFlag.ExplicitFlush flag. Flushes the changes, that is for
         * buffers mapped with the MappingFlag.Coherent, changes will be visible
         * to the server right away, otherwise an additional
         * memoryBarrier(ClientMappedBuffers) call is necessary
         */
        public void flush() {
            flush(offset, length);
        }

        /**
         * Only works with buffers that have been mapped using the
         * MappingFlag.ExplicitFlush flag.Flushes the changes, that is for
         * buffers mapped with the MappingFlag.Coherent, changes will be visible
         * to the server right away, otherwise an additional
         * memoryBarrier(ClientMappedBuffers) call is necessary
         *
         * @param offset offset in bytes from the beginning of the mapped region
         * to start flushing
         * @param length length in bytes to start flushing
         */
        public void flush(int offset, int length) {
            if ((flags & MappingFlag.ExplicitFlush.getGlConstant()) == 0) {
                throw new IllegalStateException("The mapping has not been established with the MappingFlag.Explicit flush and thus cannot explicitly flush");
            }
            untypedBuffer.RENDERER.flushMappedBuffer(this, offset, length);
        }

        /**
         * Returns the raw bffer that can be used to write data directly to GL
         *
         * @return the buffer to access GL memory
         */
        public ByteBuffer getRawData() {
            return buffer;
        }

        /**
         * Returns the UntypedBuffer instance this BufferMappingHandle is
         * handling the mapping for
         *
         * @return the UntypedBuffer that is accessed by this mapping
         */
        public UntypedBuffer getBuffer() {
            return untypedBuffer;
        }

        /**
         * Unmaps the buffer
         */
        public void unmap() {
            untypedBuffer.RENDERER.unmapBuffer(this);
            untypedBuffer.mappingHandle = null;
        }
    }

    //TYPED VIEWS ON THIS BUFFER
    /**
     * Returns an AtomicCounterBuffer view on this UntypedBuffer.
     * AtomicCounterBuffers need to have their binding explicitly set in the
     * shader, specify the same binding here to connect to the related buffer
     *
     * @param binding the binding specified in the shader
     * @return a new AtomicCounterBuffer view on this buffer
     */
    public AtomicCounterBuffer asAtomicCounterBuffer(int binding) {
        if (!initialized) {
            throw new UnsupportedOperationException("Cannot create typed view on this buffer when it is not yet initialized");
        }
        if (gpuSize < 4) {
            throw new UnsupportedOperationException("To create an AtomicCounterBuffer, the UntypedBuffers size must be at least 4 bytes (1 int)");
        }
        return new AtomicCounterBuffer(this, binding);
    }

    /**
     * Creates a DispatchIndrectBuffer view on this UntypedBuffer.
     * DispatchIndirectBuffers can be used to store work group invocation counts
     * for ComputeShader dispatches
     *
     * @return a new DispatchIndirectBuffer view on this buffer
     */
    public DispatchIndirectBuffer asDispatchIndirectBuffer() {
        if (!initialized) {
            throw new UnsupportedOperationException("Cannot create typed view on this buffer when it is not yet initialized");
        }
        if (gpuSize < 12) {
            throw new UnsupportedOperationException("To create an DispatchIndirectBuffer, the UntypedBuffers size must be at least 12 bytes (3 ints)");
        }
        return new DispatchIndirectBuffer(this);
    }

    /**
     * Creates a DrawIndirectBuffer view on this
     * UntypedBuffer.DrawIndirectBuffers can be used to store large number of
     * draw commands on the GPU to reduce driver overhead or to do some sort of
     * dynamic batching and instancing with a single draw call.
     *
     * @param mode the DrawIndirectMode to use (to specify using IndexBuffers or
     * not)
     * @return a new DrawIndirectBuffer view on this buffer
     */
    public DrawIndirectBuffer asDrawIndirectBuffer(DrawIndirectBuffer.DrawIndirectMode mode) {
        if (!initialized) {
            throw new UnsupportedOperationException("Cannot create typed view on this buffer when it is not yet initialized");
        }
        if (gpuSize < (mode == DrawIndirectBuffer.DrawIndirectMode.Draw ? 16 : 20)) {
            throw new UnsupportedOperationException("To create an DrawIndirectBuffer, the UntypedBuffers size must be at least 16/20 bytes (4/5 ints)");
        }
        return new DrawIndirectBuffer(this, mode);
    }

    /**
     * Creates a new QueryBuffer view on this UntypedBuffer. QueryBuffers can be
     * used to store the result of a QueryObject and make it available to
     * shaders without having to read back the result to the CPU. Useful for
     * Culling on the GPU or dynamically adjusting shader quality dependant on
     * the query result without having to talk to the CPU
     *
     * @return a new QueryBuffer view on this buffer
     */
    public QueryBuffer asQueryBuffer() {
        if (!initialized) {
            throw new UnsupportedOperationException("Cannot create typed view on this buffer when it is not yet initialized");
        }
        if (gpuSize < 4) {
            throw new UnsupportedOperationException("To create an QueryBuffer, the UntypedBuffers size must be at least 4 bytes (1 int)");
        }
        return new QueryBuffer(this);
    }

    /**
     * Creates a new ShaderStorageBuffer (SSBO) view on this UntypedBuffer.
     * SSBOs can be used to store large amounts of data on the GPU and make them
     * available to shaders.
     *
     * @param writer null for autolayout, otherwise specify to do manual layout
     * @return a new ShaderStorageBuffer view on this buffer
     */
    public ShaderStorageBuffer asShaderStorageBuffer(FieldBuffer.FieldBufferWriter writer) {
        if (!initialized) {
            throw new UnsupportedOperationException("Cannot create typed view on this buffer when it is not yet initialized");
        }
        return new ShaderStorageBuffer(this, writer);
    }

    /**
     * Creates a new UniformBuffer (UBO) view on this UntypedBuffer.UBOs are
     * used to combine several uniform uploads into a single call and to reduce
     * the amount of data to upload by sharing the content between shaders. If
     * you have a Uniform that most shaders need (say LightDirection and
     * LightColor), instead of uploading 2 Uniforms for each mesh to draw, you
     * can upload a buffer containing both fields once and use them in all draw
     * commands. If writer is null, GL will be queried for the layout of the
     * buffer and the fields will be written accordingly. Otherwise the writer
     * will be used to write fields into the buffer
     *
     * @param writer null for autolayout, otherwise specify to do manual layout
     * @return a new UniformBuffer view on this buffer
     */
    public UniformBuffer asUniformBuffer(FieldBuffer.FieldBufferWriter writer) {
        if (!initialized) {
            throw new UnsupportedOperationException("Cannot create typed view on this buffer when it is not yet initialized");
        }
        return new UniformBuffer(this, writer);
    }

    /**
     * Creates a new IndexBuffer (IBO) view on this UntypedBuffer. IBOs tell how
     * to connect the vertices in the VertexBuffers, similar to "Connect the
     * Dots"
     *
     * @param format format of the IBO (either unsigned byte, unsigned short or
     * unsigned int)
     * @return a new IndexBuffer view on this buffer
     */
    public VertexBuffer asIndexBuffer(VertexBuffer.Format format) {
        return asVertexBuffer(VertexBuffer.Type.Index, format, 1, 0, 0);
    }

    /**
     * Creates a new VertexBuffer (VBO) view on this UntypedBuffer. VBOs tell
     * which data will be available to the VertexShader and how to interpret
     * that data.Useful especially if you also have a ShaderStorageBuffer view
     * on this same UntypedBuffer, update the data of this UntypedBuffer via the
     * ShaderStorageBuffer-view in a ComputeShader and use it in your
     * VertexShader via VBO to render what was just calculated in the
     * ComputeShader (think ParticleSystem, any sort of simulation / animation
     * etc)
     *
     * @param type the type of the VertexBuffer
     * @param format the format of the VertexBuffer
     * @param components the number of its components
     * @param stride the stride in bytes for one block of vertex data
     * @param offset the offset into the block of vertex data, this VertexBuffer
     * starts from
     * @return a new VertexBuffer view on this buffer
     */
    public VertexBuffer asVertexBuffer(VertexBuffer.Type type, VertexBuffer.Format format, int components, int stride, int offset) {
        if (!initialized) {
            throw new UnsupportedOperationException("Cannot create typed view on this buffer when it is not yet initialized");
        }

        VertexBuffer vb = new VertexBuffer(type);
        vb.setupData(components, format, this);
        vb.setOffset(offset);
        vb.setStride(stride);
        return vb;
    }

    /**
     * Used by QueryBuffers to directly access the renderer for more
     * userfriendly QueryBuffer usage like queryBuffer.storeResult(query);
     *
     * @return
     */
    protected Renderer getRenderer() {
        return RENDERER;
    }

    //NATIVE OBJECT METHODS
    @Override
    public void resetObject() {
        id = -1;
        setUpdateNeeded();
    }

    @Override
    public void deleteObject(final Object rendererObject) {
        if (!(rendererObject instanceof Renderer)) {
            throw new IllegalArgumentException("This bo can't be deleted from " + rendererObject);
        }
        // ((Renderer) rendererObject).deleteBuffer(this);  //TODO add to renderer
    }

    @Override
    public NativeObject createDestructableClone() {
        return new UntypedBuffer(getId(), MEM_MODE, BUFFERDATA_USAGE, STORAGE_FLAGS, RENDERER);
    }

    @Override
    protected void deleteNativeBuffers() {
        super.deleteNativeBuffers();
        if (cpuData != null) {
            BufferUtils.destroyDirectBuffer(cpuData);
            cpuData = null;
        }
    }

    @Override
    public long getUniqueId() {
        return ((long) OBJTYPE_BO << 32) | ((long) id);
    }

}
